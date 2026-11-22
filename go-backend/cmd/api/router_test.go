package main

import (
	"context"
	"database/sql"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"

	"github.com/DarkAbhi/life-backend/internal/gym"
	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/jackc/pgx/v5/pgxpool"
)

type testEnv struct {
	DB       *sql.DB
	Pool     *pgxpool.Pool
	Shutdown func()
}

func startPostgres(t *testing.T) *testEnv {
	t.Helper()

	db, dsn, stop := testhelper.StartPostgresWithDSN(t)
	pool, err := pgxpool.New(context.Background(), dsn)
	if err != nil {
		stop()
		t.Fatal(err)
	}
	return &testEnv{
		DB: db, Pool: pool,
		Shutdown: func() {
			pool.Close()
			stop()
		},
	}
}

func TestHealthEndpoints(t *testing.T) {
	env := startPostgres(t)
	defer env.Shutdown()

	api := &API{DB: env.Pool}
	router := api.Router()

	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/healthz", nil)
	router.ServeHTTP(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("healthz code=%d body=%s", rec.Code, rec.Body.String())
	}

	rec2 := httptest.NewRecorder()
	req2 := httptest.NewRequest(http.MethodGet, "/readyz", nil)
	router.ServeHTTP(rec2, req2)
	if rec2.Code != http.StatusOK {
		t.Fatalf("readyz code=%d body=%s", rec2.Code, rec2.Body.String())
	}
}

func TestWorkoutAndRemovedActivityEndpoints(t *testing.T) {
	env := startPostgres(t)
	defer env.Shutdown()
	api := &API{
		DB:  env.Pool,
		Gym: gym.NewHandler(gym.NewService(gym.NewRepository(env.Pool)), func(*http.Request) (int64, error) { return 0, sql.ErrNoRows }),
	}
	router := api.Router()
	for _, tc := range []struct {
		path   string
		status int
	}{
		{"/api/workout/today", http.StatusUnauthorized},
		{"/api/meditation/today", http.StatusNotFound},
		{"/api/sport/today", http.StatusNotFound},
	} {
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, httptest.NewRequest(http.MethodPost, tc.path, nil))
		if rec.Code != tc.status {
			t.Fatalf("POST %s: got %d, want %d: %s", tc.path, rec.Code, tc.status, rec.Body.String())
		}
	}
	var count int
	if err := env.DB.QueryRow("SELECT COUNT(*) FROM gym_visits").Scan(&count); err != nil || count != 0 {
		t.Fatalf("gym_visits count = %d, err = %v", count, err)
	}
}

func TestPulseVersion(t *testing.T) {
	manifest, err := filepath.Abs("../../../release.json")
	if err != nil {
		t.Fatal(err)
	}
	expected, err := os.ReadFile(manifest)
	if err != nil {
		t.Fatal(err)
	}
	pool, err := pgxpool.New(context.Background(), "postgres://localhost/pulse")
	if err != nil {
		t.Fatal(err)
	}
	defer pool.Close()
	t.Setenv("PULSE_RELEASE_FILE", manifest)
	router := (&API{DB: pool}).Router()
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/api/version", nil))
	if rec.Code != http.StatusOK || rec.Body.String() != string(expected) {
		t.Fatalf("version code=%d body=%s", rec.Code, rec.Body.String())
	}
	if rec.Header().Get("Content-Type") != "application/json" {
		t.Fatal("version must return JSON")
	}
	t.Setenv("PULSE_RELEASE_FILE", filepath.Join(t.TempDir(), "missing.json"))
	rec = httptest.NewRecorder()
	router.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/api/version", nil))
	if rec.Code != http.StatusServiceUnavailable {
		t.Fatalf("missing manifest code=%d", rec.Code)
	}
}
