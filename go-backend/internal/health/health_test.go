package health

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/jackc/pgx/v5/pgxpool"
)

func TestLiveness(t *testing.T) {
	h := New(nil)
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/healthz", nil)

	h.Liveness(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected 200, got %d", rec.Code)
	}
}

func TestReadyz(t *testing.T) {
	_, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	pool, err := pgxpool.New(context.Background(), dsn)
	if err != nil {
		t.Fatal(err)
	}
	defer pool.Close()
	h := New(pool)
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/readyz", nil)

	h.Readyz(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected 200, got %d", rec.Code)
	}
}

func TestReadyzDoesNotExposeDatabaseError(t *testing.T) {
	pool, err := pgxpool.New(context.Background(), "postgres://user:pass@localhost:5432/life")
	if err != nil {
		t.Fatal(err)
	}
	pool.Close()
	rec := httptest.NewRecorder()
	New(pool).Readyz(rec, httptest.NewRequest(http.MethodGet, "/readyz", nil))
	if rec.Code != http.StatusServiceUnavailable {
		t.Fatalf("expected 503, got %d", rec.Code)
	}
	var body map[string]string
	if err := json.NewDecoder(rec.Body).Decode(&body); err != nil {
		t.Fatal(err)
	}
	if len(body) != 1 || body["status"] != "db not ready" {
		t.Fatalf("unexpected readiness response: %v", body)
	}
}
