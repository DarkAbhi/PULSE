//go:build integration

package quests

import (
	"context"
	"testing"

	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
)

func TestSettingsViewPrefersTodayOverQueuedRows(t *testing.T) {
	db, dsn, stop := testhelper.StartPostgresWithDSN(t)
	t.Cleanup(stop)

	ctx := context.Background()
	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(pool.Close)
	repo := NewRepository(pool)

	var userID int64
	var today, tomorrow string
	if err := db.QueryRow(`
		INSERT INTO users (username, password_hash) VALUES ('test-settings-effective', 'test')
		RETURNING id, (clock_timestamp() AT TIME ZONE 'UTC')::date::text,
		          ((clock_timestamp() AT TIME ZONE 'UTC')::date + 1)::text
	`).Scan(&userID, &today, &tomorrow); err != nil {
		t.Fatal(err)
	}
	insert := func(day string, steps int) {
		t.Helper()
		if _, err := db.Exec(`
			INSERT INTO user_quest_settings (user_id, effective_date, gym_workout, activity_rings, step_target)
			VALUES ($1, $2::date, false, false, $3)
		`, userID, day, steps); err != nil {
			t.Fatal(err)
		}
	}
	read := func() SettingsView {
		t.Helper()
		view, err := repo.Settings(ctx, userID)
		if err != nil {
			t.Fatal(err)
		}
		return view
	}

	// Only a queued row: still configured, labelled with its future date.
	insert(tomorrow, 9000)
	if view := read(); !view.Configured || view.SettingsEffective != tomorrow ||
		view.Settings.StepTarget == nil || *view.Settings.StepTarget != 9000 {
		t.Fatalf("queued-only view = %+v", view)
	}

	// A row in effect today wins over the queued one.
	insert(today, 4000)
	if view := read(); view.SettingsEffective != today ||
		view.Settings.StepTarget == nil || *view.Settings.StepTarget != 4000 {
		t.Fatalf("current view = %+v", view)
	}
}
