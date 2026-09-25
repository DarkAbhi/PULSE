package purchase

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

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/go-chi/chi/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/DarkAbhi/life-backend/internal/timeutil"
)

func loginUser(t *testing.T, db *sql.DB) *http.Cookie {
	t.Helper()
	token := "purchasetesttoken"
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

func TestNextMonthPurchases(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	pool, poolErr := pgxpool.New(context.Background(), dsn)
	if poolErr != nil {
		t.Fatal(poolErr)
	}
	defer pool.Close()
	h := NewHandler(NewService(NewRepository(pool)), func(r *http.Request) (int64, error) { user, err := auth.GetSessionUser(db, r); return user.ID, err })
	cookie := loginUser(t, db)

	// Seed purchases
	month := timeutil.NextMonthDate()
	_, err := db.Exec(`
		INSERT INTO next_month_purchases (user_id, target_month, name, price, url)
		VALUES 
		(1, $1::date, 'Keyboard', 150.00, 'http://kbd.com'),
		(1, $1::date, 'Mouse', 80.00, NULL)
	`, month)
	if err != nil {
		t.Fatalf("failed to seed purchases: %v", err)
	}

	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/next-month-purchases", nil)
	req.AddCookie(cookie)
	h.NextMonthPurchases(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected 200 OK, got %d", rec.Code)
	}

	var out map[string]any
	_ = json.NewDecoder(rec.Body).Decode(&out)
	items, ok := out["items"].([]any)
	if !ok || len(items) != 2 {
		t.Errorf("expected 2 items, got %v", out["items"])
	}
	if out["total"].(float64) != 230.00 {
		t.Errorf("expected total 230.00, got %v", out["total"])
	}
}

func TestCreateNextMonthPurchase(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	pool, poolErr := pgxpool.New(context.Background(), dsn)
	if poolErr != nil {
		t.Fatal(poolErr)
	}
	defer pool.Close()
	h := NewHandler(NewService(NewRepository(pool)), func(r *http.Request) (int64, error) { user, err := auth.GetSessionUser(db, r); return user.ID, err })
	cookie := loginUser(t, db)

	// 1. Create - Invalid JSON
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPost, "/next-month-purchases", bytes.NewReader([]byte("{invalid")))
		req.AddCookie(cookie)
		h.CreateNextMonthPurchase(rec, req)
		if rec.Code != http.StatusBadRequest {
			t.Errorf("expected 400 Bad Request, got %d", rec.Code)
		}
	}

	// 2. Create - Empty name
	{
		rec := httptest.NewRecorder()
		body, _ := json.Marshal(purchaseInput{Name: "", Price: 10.0})
		req := httptest.NewRequest(http.MethodPost, "/next-month-purchases", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.CreateNextMonthPurchase(rec, req)
		if rec.Code != http.StatusBadRequest {
			t.Errorf("expected 400 Bad Request, got %d", rec.Code)
		}
	}

	// 3. Create - Negative price
	{
		rec := httptest.NewRecorder()
		body, _ := json.Marshal(purchaseInput{Name: "Book", Price: -5.0})
		req := httptest.NewRequest(http.MethodPost, "/next-month-purchases", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.CreateNextMonthPurchase(rec, req)
		if rec.Code != http.StatusBadRequest {
			t.Errorf("expected 400 Bad Request, got %d", rec.Code)
		}
	}

	// 4. Create - Success
	{
		rec := httptest.NewRecorder()
		urlStr := "http://book.com"
		body, _ := json.Marshal(purchaseInput{Name: "Go Book", Price: 49.99, URL: &urlStr})
		req := httptest.NewRequest(http.MethodPost, "/next-month-purchases", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.CreateNextMonthPurchase(rec, req)

		if rec.Code != http.StatusCreated {
			t.Errorf("expected 21 Created, got %d", rec.Code)
		}

		var out purchaseDTO
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if out.Name != "Go Book" || out.Price != 49.99 || out.URL == nil || *out.URL != urlStr {
			t.Errorf("unexpected response: %+v", out)
		}
	}
}

func TestDeleteNextMonthPurchase(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	pool, poolErr := pgxpool.New(context.Background(), dsn)
	if poolErr != nil {
		t.Fatal(poolErr)
	}
	defer pool.Close()
	h := NewHandler(NewService(NewRepository(pool)), func(r *http.Request) (int64, error) { user, err := auth.GetSessionUser(db, r); return user.ID, err })
	cookie := loginUser(t, db)

	// Seed one purchase
	var purchaseID int64
	month := timeutil.NextMonthDate()
	err := db.QueryRow(`
		INSERT INTO next_month_purchases (user_id, target_month, name, price)
		VALUES (1, $1::date, 'Keyboard', 150.00)
		RETURNING id
	`, month).Scan(&purchaseID)
	if err != nil {
		t.Fatalf("failed to seed: %v", err)
	}

	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodDelete, "/next-month-purchases/{id}", nil)
	req.AddCookie(cookie)

	rctx := chi.NewRouteContext()
	rctx.URLParams.Add("id", strconv.FormatInt(purchaseID, 10))
	req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

	h.DeleteNextMonthPurchase(rec, req)

	if rec.Code != http.StatusNoContent {
		t.Errorf("expected 204 No Content, got %d", rec.Code)
	}

	// Verify DB deleted
	var count int
	_ = db.QueryRow(`SELECT COUNT(*) FROM next_month_purchases WHERE id = $1`, purchaseID).Scan(&count)
	if count != 0 {
		t.Errorf("expected purchase to be deleted, got count %d", count)
	}
}

func TestClearNextMonthPurchases(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	pool, poolErr := pgxpool.New(context.Background(), dsn)
	if poolErr != nil {
		t.Fatal(poolErr)
	}
	defer pool.Close()
	h := NewHandler(NewService(NewRepository(pool)), func(r *http.Request) (int64, error) { user, err := auth.GetSessionUser(db, r); return user.ID, err })
	cookie := loginUser(t, db)

	// Seed purchases
	month := timeutil.NextMonthDate()
	_, err := db.Exec(`
		INSERT INTO next_month_purchases (user_id, target_month, name, price)
		VALUES 
		(1, $1::date, 'Keyboard', 150.00),
		(1, $1::date, 'Mouse', 80.00)
	`, month)
	if err != nil {
		t.Fatalf("failed to seed: %v", err)
	}

	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodDelete, "/next-month-purchases", nil)
	req.AddCookie(cookie)
	h.ClearNextMonthPurchases(rec, req)

	if rec.Code != http.StatusNoContent {
		t.Errorf("expected 204 No Content, got %d", rec.Code)
	}

	var count int
	_ = db.QueryRow(`SELECT COUNT(*) FROM next_month_purchases WHERE user_id = 1 AND target_month = $1::date`, month).Scan(&count)
	if count != 0 {
		t.Errorf("expected all purchases cleared, got count %d", count)
	}
}
