//go:build integration

package quests

import (
	"context"
	"testing"
	"time"

	"github.com/DarkAbhi/life-backend/internal/gym"
	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/source/file"
	"github.com/jackc/pgx/v5/pgxpool"
)

func TestWorkoutScheduleChangesReconcileToday(t *testing.T) {
	db, dsn, stop := testhelper.StartPostgresWithDSN(t)
	t.Cleanup(stop)

	ctx := context.Background()
	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(pool.Close)
	repo := NewRepository(pool)
	schedule := gym.NewService(gym.NewRepository(pool))

	migrations, err := migrate.New("file:../../migrations", dsn)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _, _ = migrations.Close() })
	step := func(count int) {
		t.Helper()
		if err := migrations.Steps(count); err != nil {
			t.Fatal(err)
		}
	}

	var userID int64
	err = db.QueryRow(`
		INSERT INTO users (username, password_hash) VALUES ('test-workout-quest-schedule', 'test') RETURNING id
	`).Scan(&userID)
	if err != nil {
		t.Fatal(err)
	}
	exec := func(query string, args ...any) {
		t.Helper()
		if _, err := db.Exec(query, args...); err != nil {
			t.Fatal(err)
		}
	}
	exec("INSERT INTO user_profiles (user_id, display_name, timezone) VALUES ($1, 'Test', 'Asia/Kolkata')", userID)
	overview := func() Overview {
		t.Helper()
		got, err := repo.Overview(ctx, userID, time.Time{})
		if err != nil {
			t.Fatal(err)
		}
		return got
	}
	today := overview().Today
	date, err := time.Parse(time.DateOnly, today)
	if err != nil {
		t.Fatal(err)
	}
	weekday := int16((int(date.Weekday())+6)%7 + 1)
	save := func(workoutType, focus string) {
		t.Helper()
		if err := schedule.SaveWorkoutScheduleDay(ctx, userID, weekday, workoutType, focus); err != nil {
			t.Fatal(err)
		}
	}

	save("single_muscle", "Chest")
	if _, err := repo.SaveSettings(ctx, userID, Settings{GymWorkout: true}); err != nil {
		t.Fatal(err)
	}
	var visitID, exerciseID int64
	if err := db.QueryRow("INSERT INTO gym_visits (user_id) VALUES ($1) RETURNING id", userID).Scan(&visitID); err != nil {
		t.Fatal(err)
	}
	if err := db.QueryRow(`
		INSERT INTO gym_visit_exercises (gym_visit_id, name) VALUES ($1, 'Bench press') RETURNING id
	`, visitID).Scan(&exerciseID); err != nil {
		t.Fatal(err)
	}
	exec(`
		INSERT INTO gym_exercise_sets (gym_visit_exercise_id, set_number, reps, weight)
		VALUES ($1, 1, 10, 40)
	`, exerciseID)

	// Keep a reward from the same weekday last week to detect retroactive edits.
	exec("INSERT INTO quest_days (user_id, day, timezone) VALUES ($1, $2::text::date - 7, 'Asia/Kolkata')", userID, today)
	exec(`
		INSERT INTO daily_quests (user_id, day, quest_id, target, progress, xp, hearts)
		VALUES ($1, $2::text::date - 7, 'gym_workout', 1, 1, 30, 2)
	`, userID, today)
	exec(`
		INSERT INTO quest_reward_ledger (user_id, day, quest_id, xp, hearts)
		VALUES ($1, $2::text::date - 7, 'gym_workout', 30, 2)
	`, userID, today)

	check := func(hasWorkout bool) {
		t.Helper()
		// Inspect the records before the overview can reconcile a stale quest.
		var quests, xp, hearts int
		err := db.QueryRow(`
			SELECT count(q.quest_id), COALESCE(sum(r.xp), 0), COALESCE(sum(r.hearts), 0)
			FROM daily_quests q LEFT JOIN quest_reward_ledger r USING (user_id, day, quest_id)
			WHERE q.user_id = $1 AND q.day = $2::text::date AND q.quest_id = 'gym_workout'
		`, userID, today).Scan(&quests, &xp, &hearts)
		wantQuests, wantXP, wantHearts := 0, 0, 0
		if hasWorkout {
			wantQuests, wantXP, wantHearts = 1, 30, 2
		}
		if err != nil || quests != wantQuests || xp != wantXP || hearts != wantHearts {
			t.Fatalf("stored workout quest, XP and hearts = (%d, %d, %d), err = %v, want (%d, %d, %d)",
				quests, xp, hearts, err, wantQuests, wantXP, wantHearts)
		}
		got := overview()
		if len(got.TodayQuests) != wantQuests || (hasWorkout && !got.TodayQuests[0].Completed) ||
			got.Progress.TodayXP != wantXP || got.Progress.LifetimeXP != wantXP+30 || got.Progress.Hearts != wantHearts+2 {
			t.Fatalf("unexpected workout quest or rewards: %+v", got)
		}
	}

	check(true)
	step(-1)
	save("rest_day", "")
	check(true)
	step(1)
	// The dashboard read reconciles a stale workout quest without losing past completions.
	activity, err := repo.Activity(ctx, userID)
	if err != nil {
		t.Fatal(err)
	}
	if activity.Today != today || len(activity.Days) != 1 ||
		activity.Days[0].Date != date.AddDate(0, 0, -7).Format(time.DateOnly) ||
		len(activity.Days[0].CompletedIDs) != 1 || activity.Days[0].CompletedIDs[0] != "gym_workout" {
		t.Fatalf("expected activity to include only the past workout: %+v", activity)
	}
	check(false)

	save("single_muscle", "Chest")
	check(true)
	save("rest_day", "")
	check(false)
	save("single_muscle", "Chest")
	check(true)
	save("", "")
	check(false)
}
