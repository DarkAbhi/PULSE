package horizon

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

func budgetsLoginUser(t *testing.T, db *sql.DB) *http.Cookie {
	t.Helper()
	token := "budgetstesttoken"
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

func TestBudgetsCRUD(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()
	pool := testPool(t, dsn)
	defer pool.Close()

	h := NewBudgetsHandler(pool, testSessionLookup(db))
	cookie := budgetsLoginUser(t, db)

	var budgetID int64
	{
		body, _ := json.Marshal(BudgetInput{Name: "Housing & Bills", AllocatedAmount: 50000.0})
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPost, "/api/horizon/budgets", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.CreateBudget(rec, req)

		if rec.Code != http.StatusCreated {
			t.Fatalf("expected 201 Created, got %d", rec.Code)
		}

		var b BudgetDTO
		_ = json.NewDecoder(rec.Body).Decode(&b)
		budgetID = b.ID
		if b.AllocatedAmount != 50000.0 || b.AvailableAmount != 50000.0 || b.UsedAmount != 0 {
			t.Errorf("unexpected initial budget DTO: %+v", b)
		}
	}

	{
		body, _ := json.Marshal(BudgetInput{Name: "Housing & Utilities", AllocatedAmount: 60000.0})
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPut, "/api/horizon/budgets/{id}", bytes.NewReader(body))
		req.AddCookie(cookie)

		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(budgetID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.UpdateBudget(rec, req)
		if rec.Code != http.StatusOK {
			t.Fatalf("expected 200 OK, got %d", rec.Code)
		}
	}

	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodDelete, "/api/horizon/budgets/{id}", nil)
		req.AddCookie(cookie)

		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(budgetID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.DeleteBudget(rec, req)
		if rec.Code != http.StatusNoContent {
			t.Fatalf("expected 204 No Content, got %d", rec.Code)
		}
	}
}
