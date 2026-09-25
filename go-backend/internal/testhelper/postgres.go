// Package testhelper provides PostgreSQL-backed integration test infrastructure.
package testhelper

import (
	"context"
	"database/sql"
	"fmt"
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

// StartPostgres starts a temporary postgres container, runs migrations, and returns the DB connection and a cleanup function.
func StartPostgres(t *testing.T) (*sql.DB, func()) {
	db, _, stop := StartPostgresWithDSN(t)
	return db, stop
}

// StartPostgresWithDSN also returns the connection string for pgxpool-backed slices.
func StartPostgresWithDSN(t *testing.T) (*sql.DB, string, func()) {
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
	dsn := base + "?sslmode=disable"

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

	// Find the migrations directory by traversing upwards
	wd, _ := os.Getwd()
	startWd := wd
	var migrationsPath string
	for i := 0; i < 5; i++ {
		path := filepath.Clean(filepath.Join(wd, "migrations"))
		if _, err := os.Stat(path); err == nil {
			migrationsPath = path
			break
		}
		pathUp := filepath.Clean(filepath.Join(wd, "../migrations"))
		if _, err := os.Stat(pathUp); err == nil {
			migrationsPath = pathUp
			break
		}
		wd = filepath.Dir(wd)
	}
	if migrationsPath == "" {
		migrationsPath = filepath.Clean(filepath.Join(startWd, "../../migrations"))
	}
	srcURL := "file://" + migrationsPath

	m, err := migrate.New(srcURL, dsn)
	if err != nil {
		t.Fatalf("migrate.New for path %s: %v", srcURL, err)
	}
	if err := m.Up(); err != nil && err != migrate.ErrNoChange {
		t.Fatalf("migrate up: %v", err)
	}

	return db, dsn, func() {
		_ = db.Close()
		_ = container.Terminate(ctx)
	}
}
