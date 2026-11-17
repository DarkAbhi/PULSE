//go:build integration

package statementimport

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
	"testing"

	horizonquery "github.com/DarkAbhi/life-backend/internal/horizon/query"
	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/go-chi/chi/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type extractorStub func(*http.Request) (*http.Response, error)

func (f extractorStub) RoundTrip(r *http.Request) (*http.Response, error) { return f(r) }

func TestPostgresImportOwnershipAndConcurrentOverlap(t *testing.T) {
	db, dsn, cleanup := testhelper.StartPostgresWithDSN(t)
	defer cleanup()
	pool, err := pgxpool.New(t.Context(), dsn)
	if err != nil {
		t.Fatal(err)
	}
	defer pool.Close()

	newUser := func(name string) int64 {
		t.Helper()
		var user int64
		if err := db.QueryRow("INSERT INTO users(username, password_hash) VALUES ($1,'test') RETURNING id", name).Scan(&user); err != nil {
			t.Fatal(err)
		}
		if _, err := db.Exec("INSERT INTO user_profiles(user_id,display_name,currency) VALUES ($1,'','INR')", user); err != nil {
			t.Fatal(err)
		}
		return user
	}
	owner := newUser("statement-owner")
	foreign := newUser("statement-foreign")
	var account int64
	if err := db.QueryRow(`INSERT INTO financial_accounts
		(user_id,display_name,account_type)
		VALUES ($1,'Test account','bank_account') RETURNING id`, owner).Scan(&account); err != nil {
		t.Fatal(err)
	}
	if _, err := db.Exec(`INSERT INTO bank_accounts (account_id,institution_name,last_four,holder_name,holder_email,bank_account_subtype) VALUES ($1,'Kotak','4321','A Person','person@example.com','savings')`, account); err != nil {
		t.Fatal(err)
	}
	document, err := json.Marshal(syntheticDocument("20.00", "5.00"))
	if err != nil {
		t.Fatal(err)
	}
	handler := NewHandler(pool, func(r *http.Request) (int64, error) {
		if r.Header.Get("X-Test-Owner") == "foreign" {
			return foreign, nil
		}
		return owner, nil
	}, "http://statement-extractor")
	handler.svc.extractor.client.Transport = extractorStub(func(*http.Request) (*http.Response, error) {
		return &http.Response{StatusCode: 200, Body: io.NopCloser(bytes.NewReader(document))}, nil
	})
	router := chi.NewRouter()
	router.Route("/api", handler.RegisterRoutes)
	const path = "/api/horizon/statement-imports"

	postPDF := func(variant int) *http.Request {
		var body bytes.Buffer
		form := multipart.NewWriter(&body)
		_ = form.WriteField("account_id", fmt.Sprint(account))
		part, err := form.CreateFormFile("file", "statement.pdf")
		if err != nil {
			t.Fatal(err)
		}
		_, _ = fmt.Fprintf(part, "%%PDF-1.7 variant %d", variant)
		_ = form.Close()
		req := httptest.NewRequest("POST", path, &body)
		req.Header.Set("Content-Type", form.FormDataContentType())
		return req
	}

	foreignUpload := postPDF(0)
	foreignUpload.Header.Set("X-Test-Owner", "foreign")
	res := httptest.NewRecorder()
	router.ServeHTTP(res, foreignUpload)
	if res.Code != 404 {
		t.Fatalf("foreign upload status %d", res.Code)
	}

	var previews [2]batchSummary
	for i := range previews {
		res := httptest.NewRecorder()
		router.ServeHTTP(res, postPDF(i))
		if res.Code != 201 {
			t.Fatalf("upload status %d", res.Code)
		}
		if err := json.Unmarshal(res.Body.Bytes(), &previews[i]); err != nil || previews[i].Counts["parsed"] != 2 {
			t.Fatal("invalid batch preview")
		}
	}
	if previews[0].ID == previews[1].ID {
		t.Fatal("distinct source files must have distinct drafts")
	}

	foreignRead := httptest.NewRequest("GET", path+"/"+previews[0].ID+"/rows", nil)
	foreignRead.Header.Set("X-Test-Owner", "foreign")
	res = httptest.NewRecorder()
	router.ServeHTTP(res, foreignRead)
	if res.Code != 404 {
		t.Fatalf("foreign preview status %d", res.Code)
	}
	edited := httptest.NewRequest("PATCH", path+"/"+previews[0].ID+"/rows",
		bytes.NewBufferString(`{"revision":1,"changes":[{"id":1,"amount":"19.99","included":false}]}`))
	res = httptest.NewRecorder()
	router.ServeHTTP(res, edited)
	if res.Code != 422 {
		t.Fatalf("amount edit bypassed reconciliation with status %d", res.Code)
	}

	// A profile change after upload must block committing the old-currency draft.
	if _, err := db.Exec("UPDATE user_profiles SET currency='USD' WHERE user_id=$1", owner); err != nil {
		t.Fatal(err)
	}
	currencyResult := httptest.NewRecorder()
	router.ServeHTTP(currencyResult, httptest.NewRequest("POST", path+"/"+previews[0].ID+"/commit", bytes.NewBufferString(`{"revision":1}`)))
	if currencyResult.Code != 422 || !strings.Contains(currencyResult.Body.String(), "CURRENCY_MISMATCH") {
		t.Fatalf("currency change bypassed: %d %s", currencyResult.Code, currencyResult.Body.String())
	}
	if _, err := db.Exec("UPDATE user_profiles SET currency='INR' WHERE user_id=$1", owner); err != nil {
		t.Fatal(err)
	}

	var wg sync.WaitGroup
	var results [2]commitResult
	for i, b := range previews {
		wg.Go(func() {
			res := httptest.NewRecorder()
			router.ServeHTTP(res, httptest.NewRequest("POST", path+"/"+b.ID+"/commit", bytes.NewBufferString(`{"revision":1}`)))
			if res.Code != 200 {
				t.Errorf("commit status %d", res.Code)
				return
			}
			if err := json.Unmarshal(res.Body.Bytes(), &results[i]); err != nil {
				t.Error("invalid commit response")
			}
		})
	}
	wg.Wait()
	if results[0].Imported+results[1].Imported != 2 || results[0].Duplicates+results[1].Duplicates != 2 {
		t.Fatal("overlap did not import exactly one copy of each transaction")
	}
	var total int
	if err := db.QueryRow("SELECT count(*) FROM financial_horizon_transactions WHERE user_id=$1", owner).Scan(&total); err != nil || total != 2 {
		t.Fatal("unexpected saved transaction count")
	}

	res = httptest.NewRecorder()
	router.ServeHTTP(res, postPDF(0))
	if res.Code != 409 {
		t.Fatalf("exact replay status %d", res.Code)
	}

	t.Run("reimport after deleting all transactions", func(t *testing.T) {
		original := 0
		if results[1].Imported > 0 {
			original = 1
		}
		var first, last int64
		if err := db.QueryRow(`SELECT min(id), max(id) FROM financial_horizon_transactions
			WHERE user_id = $1 AND import_batch_id = $2`, owner, previews[original].ID).Scan(&first, &last); err != nil {
			t.Fatal(err)
		}
		deleteTransaction := func(id int64) {
			t.Helper()
			deleted, err := horizonquery.New(pool).DeleteTransaction(t.Context(), horizonquery.DeleteTransactionParams{
				ID: id, UserID: owner,
			})
			if err != nil || deleted != 1 {
				t.Fatalf("delete returned %d rows, error %v", deleted, err)
			}
		}
		deleteTransaction(first)
		res := httptest.NewRecorder()
		router.ServeHTTP(res, postPDF(original))
		if res.Code != 409 {
			t.Fatalf("replay after partial deletion returned %d, want 409", res.Code)
		}

		deleteTransaction(last)
		res = httptest.NewRecorder()
		router.ServeHTTP(res, postPDF(2))
		if res.Code != 201 {
			t.Fatalf("overlapping PDF after full deletion returned %d, want 201", res.Code)
		}
		var overlap batchSummary
		if err := json.Unmarshal(res.Body.Bytes(), &overlap); err != nil || overlap.Counts["duplicates"] != 2 {
			t.Fatalf("expected overlapping PDF to skip both rows, got %+v, error %v", overlap, err)
		}

		res = httptest.NewRecorder()
		router.ServeHTTP(res, postPDF(original))
		if res.Code != 201 {
			t.Fatalf("replay after full deletion returned %d, want 201", res.Code)
		}
		var restored batchSummary
		if err := json.Unmarshal(res.Body.Bytes(), &restored); err != nil {
			t.Fatal(err)
		}
		if restored.ID == previews[original].ID || restored.Counts["duplicates"] != 0 {
			t.Fatalf("expected a fresh preview without duplicates, got %+v", restored)
		}

		res = httptest.NewRecorder()
		commit := httptest.NewRequest("POST", path+"/"+restored.ID+"/commit", bytes.NewBufferString(`{"revision":1}`))
		router.ServeHTTP(res, commit)
		if res.Code != 200 {
			t.Fatalf("reimport commit returned %d, want 200", res.Code)
		}
		var result commitResult
		if err := json.Unmarshal(res.Body.Bytes(), &result); err != nil || result.Imported != 2 {
			t.Fatalf("expected both deleted transactions to be imported again, got %+v, error %v", result, err)
		}
		res = httptest.NewRecorder()
		router.ServeHTTP(res, postPDF(original))
		if res.Code != 409 {
			t.Fatalf("replay after reimport returned %d, want 409", res.Code)
		}
	})

	t.Run("suggests a scoped fallback and learns the reviewed category", func(t *testing.T) {
		var investment, others, stocks int64
		err := db.QueryRow(`
			SELECT p.id, other.id, stocks.id
			FROM financial_horizon_categories p
			JOIN financial_horizon_categories other ON other.parent_id = p.id AND other.name = 'others'
			JOIN financial_horizon_categories stocks ON stocks.parent_id = p.id AND stocks.name = 'stocks'
			WHERE p.name = 'Investment' AND p.is_default`).Scan(&investment, &others, &stocks)
		if err != nil {
			t.Fatal(err)
		}

		upload := func(variant int, narration string) (batchSummary, rowPreview) {
			t.Helper()
			doc := syntheticDocument("20.00", "5.00")
			for i, word := range doc.Pages[0].Words {
				if word.Text == "Synthetic market" {
					doc.Pages[0].Words[i].Text = narration
				}
				if word.Text == "wrap line" {
					doc.Pages[0].Words[i].Text = "continued"
				}
			}
			var err error
			document, err = json.Marshal(doc)
			if err != nil {
				t.Fatal(err)
			}
			res := httptest.NewRecorder()
			router.ServeHTTP(res, postPDF(variant))
			var result struct {
				batchSummary
				previewPage
			}
			if res.Code != http.StatusCreated {
				t.Fatalf("upload status %d: %s", res.Code, res.Body)
			}
			if err := json.Unmarshal(res.Body.Bytes(), &result); err != nil || len(result.Rows) != 2 {
				t.Fatalf("invalid preview: %v", err)
			}
			return result.batchSummary, result.Rows[0]
		}

		batch, row := upload(3, "UPI/DR/123456789012/ICCL ZER/HDFC/zerodha/4r7w00ab32")
		if row.Description != "Zerodha payment" || row.CategoryID == nil || *row.CategoryID != others || !row.Included {
			t.Fatalf("unexpected first suggestion: %+v", row)
		}
		narration := row.OriginalNarration
		if !strings.Contains(narration, "123456789012") || narration == row.Description {
			t.Fatal("preview must expose the separate original narration")
		}
		body := fmt.Sprintf(`{"revision":%d,"changes":[{"id":1,"description":"Trading account funding","category_id":%d}]}`,
			batch.Revision, stocks)
		res := httptest.NewRecorder()
		router.ServeHTTP(res, httptest.NewRequest("PATCH", path+"/"+batch.ID+"/rows", bytes.NewBufferString(body)))
		var editedBatch batchSummary
		if res.Code != http.StatusOK {
			t.Fatalf("edit status %d: %s", res.Code, res.Body)
		}
		if err := json.Unmarshal(res.Body.Bytes(), &editedBatch); err != nil {
			t.Fatal(err)
		}
		res = httptest.NewRecorder()
		body = fmt.Sprintf(`{"revision":%d}`, editedBatch.Revision)
		router.ServeHTTP(res, httptest.NewRequest("POST", path+"/"+batch.ID+"/commit", bytes.NewBufferString(body)))
		if res.Code != http.StatusOK {
			t.Fatalf("commit status %d: %s", res.Code, res.Body)
		}

		var saved horizonquery.ListFilteredTransactionsRow
		for _, id := range []int64{investment, stocks} {
			rows, err := horizonquery.New(pool).ListFilteredTransactions(t.Context(), horizonquery.ListFilteredTransactionsParams{
				UserID: owner, Column4: id, Limit: 10,
			})
			if err != nil || len(rows) != 1 || rows[0].Name != "Trading account funding" ||
				rows[0].CategoryName != "Investment / stocks" || rows[0].OriginalNarration != narration {
				t.Fatalf("category %d did not find reviewed transaction: %+v, %v", id, rows, err)
			}
			saved = rows[0]
		}
		updated, err := horizonquery.New(pool).UpdateTransaction(t.Context(), horizonquery.UpdateTransactionParams{
			ID: saved.ID, UserID: owner, Name: "Revised funding name", Amount: saved.Amount, Type: saved.Type,
			TransactionDate: saved.TransactionDate, CategoryID: saved.CategoryID, CategoryName: saved.CategoryName,
		})
		if err != nil || updated.Name != "Revised funding name" || updated.OriginalNarration != narration {
			t.Fatal("editing a saved transaction must retain its original narration")
		}

		_, row = upload(4, "UPI/DR/987654321012/ZEROD HA/HDFC/zerodha/9b2w54qa15")
		if row.Description != "Zerodha payment" || row.CategoryID == nil || *row.CategoryID != stocks || !row.Included {
			t.Fatalf("unexpected learned suggestion: %+v", row)
		}
	})
}
