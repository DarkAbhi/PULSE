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
		(user_id,display_name,institution_name,account_type,currency,last_four)
		VALUES ($1,'Test account','Kotak','bank_account','INR','4321') RETURNING id`, owner).Scan(&account); err != nil {
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
}
