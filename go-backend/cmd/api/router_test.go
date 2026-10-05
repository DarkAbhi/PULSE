package main

import (
	"context"
	"database/sql"
	"fmt"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/DarkAbhi/life-backend/internal/gym"
	"github.com/jackc/pgx/v5/pgxpool"
	_ "github.com/jackc/pgx/v5/stdlib"
	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/wait"

	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
)

type testEnv struct {
	DB       *sql.DB
	Pool     *pgxpool.Pool
	Shutdown func()
}

func startPostgres(t *testing.T) *testEnv {
	t.Helper()

	ctx := context.Background()
	req := testcontainers.ContainerRequest{
		Image:        "postgres:17.6",
		ExposedPorts: []string{"5432/tcp"},
		Env: map[string]string{
			"POSTGRES_PASSWORD": "pass",
			"POSTGRES_USER":     "user",
			"POSTGRES_DB":       "testdb",
		},
		WaitingFor: wait.ForListeningPort("5432/tcp").WithStartupTimeout(60 * time.Second),
	}
	container, err := testcontainers.GenericContainer(ctx, testcontainers.GenericContainerRequest{
		ContainerRequest: req,
		Started:          true,
	})
	if err != nil {
		t.Fatalf("container start: %v", err)
	}

	host, _ := container.Host(ctx)
	port, _ := container.MappedPort(ctx, "5432/tcp")

	base := fmt.Sprintf("postgres://user:pass@%s:%s/testdb", host, port.Port())
	dsn := base + "?sslmode=disable" // <- build once

	db, err := sql.Open("pgx", dsn)
	if err != nil {
		t.Fatalf("open db: %v", err)
	}
	db.SetMaxOpenConns(5)

	ctxPing, cancel := context.WithTimeout(ctx, 10*time.Second)
	defer cancel()
	if err := db.PingContext(ctxPing); err != nil {
		t.Fatalf("ping: %v", err)
	}

	// migrations
	wd, _ := os.Getwd()
	migrationsPath := filepath.Clean(filepath.Join(wd, "../../migrations"))
	srcURL := "file://" + migrationsPath

	// IMPORTANT: pass the same DSN (no extra ?sslmode=disable appended)
	m, err := migrate.New(srcURL, dsn)
	if err != nil {
		t.Fatalf("migrate.New: %v", err)
	}
	if err := m.Up(); err != nil && err != migrate.ErrNoChange {
		t.Fatalf("migrate up: %v", err)
	}

	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatal(err)
	}
	return &testEnv{
		DB: db, Pool: pool,
		Shutdown: func() {
			pool.Close()
			_ = db.Close()
			_ = container.Terminate(ctx)
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
