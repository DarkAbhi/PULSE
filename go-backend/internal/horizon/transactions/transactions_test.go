package transactions

import (
	"bytes"
	"context"
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strconv"
	"testing"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
)

func loginUser(t *testing.T, db *sql.DB) *http.Cookie {
	t.Helper()
	token := "transactionstesttoken"
	hash := sha256.Sum256([]byte(token))
	hashStr := hex.EncodeToString(hash[:])
	expiresAt := time.Now().Add(24 * time.Hour)
	_, err := db.Exec(`
		INSERT INTO user_sessions (user_id, token_hash, expires_at)
		VALUES (1, $1, $2)
	`, hashStr, expiresAt)
	if err != nil {
		t.Fatalf("failed to insert session: %v", err)
	}
	return &http.Cookie{
		Name:  "life_session",
		Value: token,
	}
}

func TestTransactionsCRUDAndBulk(t *testing.T) {
	db, shutdown := testhelper.StartPostgres(t)
	defer shutdown()

	h := NewHandler(db)
	cookie := loginUser(t, db)

	var transactionID int64

	{
		notes := "Bought new Wireless Headphones"
		body, _ := json.Marshal(TransactionInput{
			Name:   "Sony Headphones",
			Amount: 15000.0,
			Notes:  &notes,
		})
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPost, "/api/horizon/transactions", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.CreateTransaction(rec, req)

		if rec.Code != http.StatusCreated {
			t.Fatalf("expected 201 Created, got %d", rec.Code)
		}

		var tx TransactionDTO
		_ = json.NewDecoder(rec.Body).Decode(&tx)
		if tx.Name != "Sony Headphones" || tx.Amount != 15000.0 || tx.Type != "debit" {
			t.Errorf("unexpected transaction DTO: %+v", tx)
		}
		transactionID = tx.ID
	}

	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/api/horizon/transactions", nil)
		req.AddCookie(cookie)
		h.ListTransactions(rec, req)

		if rec.Code != http.StatusOK {
			t.Fatalf("expected 200 OK, got %d", rec.Code)
		}

		var transactionsList []TransactionDTO
		_ = json.NewDecoder(rec.Body).Decode(&transactionsList)
		if len(transactionsList) != 1 {
			t.Errorf("expected 1 transaction, got %d", len(transactionsList))
		}
	}

	{
		creditType := "credit"
		bulkBody, _ := json.Marshal([]TransactionInput{
			{Name: "Bulk Item 1", Amount: 500.0},
			{Name: "Salary Credit", Amount: 1200.0, Type: &creditType},
		})
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPost, "/api/horizon/transactions/bulk", bytes.NewReader(bulkBody))
		req.AddCookie(cookie)
		h.BulkCreateTransactions(rec, req)

		if rec.Code != http.StatusCreated {
			t.Fatalf("expected 201 Created for bulk insert, got %d", rec.Code)
		}

		var bulkTx []TransactionDTO
		_ = json.NewDecoder(rec.Body).Decode(&bulkTx)
		if len(bulkTx) != 2 {
			t.Errorf("expected 2 bulk transactions, got %d", len(bulkTx))
		}
		if bulkTx[0].Type != "debit" || bulkTx[1].Type != "credit" {
			t.Errorf("unexpected bulk transaction types: %+v", bulkTx)
		}
	}

	{
		notes := "Updated price after discount"
		creditType := "credit"
		body, _ := json.Marshal(TransactionInput{
			Name:   "Sony Headphones Pro",
			Amount: 13500.0,
			Type:   &creditType,
			Notes:  &notes,
		})
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPut, "/api/horizon/transactions/{id}", bytes.NewReader(body))
		req.AddCookie(cookie)

		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(transactionID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.UpdateTransaction(rec, req)
		if rec.Code != http.StatusOK {
			t.Fatalf("expected 200 OK, got %d", rec.Code)
		}

		var tx TransactionDTO
		_ = json.NewDecoder(rec.Body).Decode(&tx)
		if tx.Amount != 13500.0 || tx.Name != "Sony Headphones Pro" || tx.Type != "credit" {
			t.Errorf("unexpected updated transaction: %+v", tx)
		}
	}

	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodDelete, "/api/horizon/transactions/{id}", nil)
		req.AddCookie(cookie)

		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(transactionID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.DeleteTransaction(rec, req)
		if rec.Code != http.StatusNoContent {
			t.Fatalf("expected 204 No Content, got %d", rec.Code)
		}
	}
}
