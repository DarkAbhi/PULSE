package activity

import (
	"bytes"
	"context"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/go-chi/chi/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
)

func TestRoutes(t *testing.T) {
	db, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	pool, err := pgxpool.New(context.Background(), dsn)
	if err != nil {
		t.Fatal(err)
	}
	defer pool.Close()
	h := NewHandler(NewService(NewRepository(pool)))
	r := chi.NewRouter()
	h.RegisterRoutes(r)
	for _, tc := range []struct {
		path     string
		body     string
		expected int
	}{
		{"/meditation/today", "", http.StatusCreated},
		{"/meditation/today", "", http.StatusBadRequest},
		{"/sport/today", `{"sport":"tennis"}`, http.StatusBadRequest},
		{"/sport/today", `{"sport":"cricket"}`, http.StatusCreated},
	} {
		rec := httptest.NewRecorder()
		r.ServeHTTP(rec, httptest.NewRequest(http.MethodPost, tc.path, bytes.NewBufferString(tc.body)))
		if rec.Code != tc.expected {
			t.Fatalf("%s: got %d, want %d: %s", tc.path, rec.Code, tc.expected, rec.Body.String())
		}
	}
	var count int
	if err := db.QueryRow(`SELECT COUNT(*) FROM meditations`).Scan(&count); err != nil || count != 1 {
		t.Fatalf("meditations: %d, %v", count, err)
	}
	if err := db.QueryRow(`SELECT COUNT(*) FROM sports`).Scan(&count); err != nil || count != 1 {
		t.Fatalf("sports: %d, %v", count, err)
	}
}
