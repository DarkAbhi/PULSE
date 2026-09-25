package main

import (
	"context"
	"database/sql"
	"fmt"
	"github.com/jackc/pgx/v5/pgxpool"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"
	"time"

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
