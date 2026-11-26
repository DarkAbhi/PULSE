//go:build integration

package gym

import (
	"context"
	"testing"
	"time"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
)

func TestFitnessTodayUsesProfileDateAndKeepsMissingDataDistinct(t *testing.T) {
	db, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	if _, err := db.Exec(`
		INSERT INTO users (id, username, password_hash) VALUES (2, 'fitness-other', 'test');
		INSERT INTO user_profiles (user_id, display_name, timezone) VALUES (1, 'Test', 'Asia/Kolkata');
		INSERT INTO fitness_activity_rings (
			user_id, summary_date, steps_count, move_calories, move_calories_goal,
			exercise_minutes, exercise_minutes_goal, stand_hours, stand_hours_goal
		) VALUES
			(1, '2026-11-22', 2000, 200, 180, 30, 30, 8, 12),
			(1, '2026-11-23', 0, 194.25, 180, 31, 30, 8, 12),
			(2, '2026-11-23', 9000, 900, 180, 60, 30, 10, 12);
		INSERT INTO fitness_sleep (user_id, sleep_date, total_asleep_seconds, main_sleep_seconds, wake_at, sessions)
		VALUES (1, '2026-11-23', 27120, 27000, '2026-11-22T19:00Z', '[{}]'),
		       (1, '2026-11-24', 25200, 25200, '2026-11-23T19:00Z', '[{}]');
	`); err != nil {
		t.Fatal(err)
	}
	service := newTestService(t, dsn)
	now := time.Date(2026, 11, 22, 20, 0, 0, 0, time.UTC)
	ctx := context.Background()

	summary, err := service.FitnessToday(ctx, 1, now)
	if err != nil {
		t.Fatal(err)
	}
	if summary.Date != "2026-11-23" || summary.StepsCount == nil || *summary.StepsCount != 0 ||
		summary.MoveCalories == nil || *summary.MoveCalories != 194.25 ||
		summary.TotalAsleepSeconds == nil || *summary.TotalAsleepSeconds != 27120 {
		t.Fatalf("unexpected daily summary: %+v", summary)
	}

	sleepOnly, err := service.FitnessToday(ctx, 1, now.AddDate(0, 0, 1))
	if err != nil {
		t.Fatal(err)
	}
	if sleepOnly.StepsCount != nil || sleepOnly.MoveCaloriesGoal != nil ||
		sleepOnly.TotalAsleepSeconds == nil || *sleepOnly.TotalAsleepSeconds != 25200 {
		t.Fatalf("unexpected sleep-only summary: %+v", sleepOnly)
	}

	other, err := service.FitnessToday(ctx, 2, now)
	if err != nil {
		t.Fatal(err)
	}
	if other.Date != "2026-11-22" || other.StepsCount != nil || other.TotalAsleepSeconds != nil {
		t.Fatalf("unexpected summary for the other user: %+v", other)
	}
}
