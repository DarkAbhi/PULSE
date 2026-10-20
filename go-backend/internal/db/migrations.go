// Package db runs the application's PostgreSQL schema migrations.
package db

import (
	"log"
	"os"
	"path/filepath"

	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
)

func migrationsPath() (string, error) {
	// Expecting ./migrations at repo root (adjust if yours differs)
	wd, err := os.Getwd()
	if err != nil {
		return "", err
	}
	// If running from cmd/, go up one
	if filepath.Base(wd) == "cmd" {
		wd = filepath.Dir(wd)
	}
	p := filepath.Join(wd, "migrations")
	return "file://" + p, nil
}

// RunMigrations applies every pending migration using the configured database.
func RunMigrations(dsn string) error {
	src, err := migrationsPath()
	if err != nil {
		return err
	}
	m, err := migrate.New(src, dsn)
	if err != nil {
		return err
	}
	defer closeSilently(m)
	if err := m.Up(); err != nil && err != migrate.ErrNoChange {
		return err
	}
	log.Println("✅ migrations up applied")
	return nil
}

// RollbackMigration reverses the most recently applied migration.
func RollbackMigration(dsn string) error {
	src, err := migrationsPath()
	if err != nil {
		return err
	}
	m, err := migrate.New(src, dsn)
	if err != nil {
		return err
	}
	defer closeSilently(m)
	if err := m.Steps(-1); err != nil {
		return err
	}
	log.Println("↩️  rolled back 1 step")
	return nil
}

// ShowMigrationVersion logs the current migration version and dirty state.
func ShowMigrationVersion(dsn string) error {
	src, err := migrationsPath()
	if err != nil {
		return err
	}
	m, err := migrate.New(src, dsn)
	if err != nil {
		return err
	}
	defer closeSilently(m)
	v, dirty, err := m.Version()
	if err == migrate.ErrNilVersion {
		log.Println("📌 version: none (no migrations applied)")
		return nil
	}
	if err != nil {
		return err
	}
	log.Printf("📌 version: %d (dirty=%v)\n", v, dirty)
	return nil
}

// RunMigrationSteps applies n migrations; a negative value rolls migrations back.
func RunMigrationSteps(dsn string, n int) error {
	src, err := migrationsPath()
	if err != nil {
		return err
	}
	m, err := migrate.New(src, dsn)
	if err != nil {
		return err
	}
	defer closeSilently(m)
	if err := m.Steps(n); err != nil {
		return err
	}
	log.Printf("🔂 applied steps: %d\n", n)
	return nil
}

func closeSilently(m *migrate.Migrate) {
	_, _ = m.Close()
}
