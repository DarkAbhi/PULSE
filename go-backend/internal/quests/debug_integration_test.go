//go:build integration

package quests

import (
	"context"
	"testing"
	"time"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/jackc/pgx/v5/pgxpool"
)

func TestDebugSeedHistoryUsesSourceRecords(t *testing.T) {
	db, dsn, stop := testhelper.StartPostgresWithDSN(t)
	t.Cleanup(stop)
	ctx := context.Background()
	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(pool.Close)
	repo := NewRepository(pool)

	var ownerID int64
	if err := db.QueryRow(`INSERT INTO users (username, password_hash)
		VALUES ('debug-history', 'test') RETURNING id`).Scan(&ownerID); err != nil {
		t.Fatal(err)
	}
	var today string
	if err := db.QueryRow(`SELECT (now() AT TIME ZONE 'UTC')::date::text`).Scan(&today); err != nil {
		t.Fatal(err)
	}
	if _, err := db.Exec(`
		INSERT INTO fitness_activity_rings (user_id, summary_date, steps_count)
		VALUES ($1, $2::text::date - 1, 42)
	`, ownerID, today); err != nil {
		t.Fatal(err)
	}

	result, err := repo.seedHistory(ctx, ownerID)
	if err != nil || result.Days != activityDays-2 || result.Completions == 0 {
		t.Fatalf("seed result = %+v, err = %v", result, err)
	}
	activity, err := repo.Activity(ctx, ownerID)
	if err != nil || len(activity.Days) == 0 {
		t.Fatalf("activity = %+v, err = %v", activity, err)
	}
	var total, maxDaily int
	for _, day := range activity.Days {
		total += len(day.CompletedIDs)
		maxDaily = max(maxDaily, len(day.CompletedIDs))
	}
	if total != result.Completions || maxDaily != 5 {
		t.Fatalf("activity completions = %d, max daily = %d, seed result = %+v", total, maxDaily, result)
	}
	overview, err := repo.Overview(ctx, ownerID, time.Time{})
	if err != nil || overview.Configured || overview.Progress.LifetimeXP == 0 || len(overview.TodayQuests) != 0 {
		t.Fatalf("seeded history must show rewards without enabling today's goals, overview = %+v, err = %v", overview, err)
	}

	var withoutSource, outsideSeed, realSteps int
	err = db.QueryRow(`
		SELECT
			(SELECT count(*) FROM quest_reward_ledger r WHERE NOT CASE r.quest_id
				WHEN 'gym_workout' THEN EXISTS (
					SELECT FROM gym_visits v JOIN gym_visit_exercises e ON e.gym_visit_id = v.id
					JOIN gym_exercise_sets s ON s.gym_visit_exercise_id = e.id
					WHERE v.user_id = r.user_id AND (v.created_at AT TIME ZONE 'UTC')::date = r.day AND s.reps > 0)
				WHEN 'daily_meals' THEN 3 = (SELECT count(*) FROM meal_plans m
					WHERE m.user_id = r.user_id AND m.date = r.day AND m.is_consumed)
				WHEN 'daily_steps' THEN EXISTS (SELECT FROM fitness_activity_rings a
					WHERE a.user_id = r.user_id AND a.summary_date = r.day AND a.steps_count >= 8000)
				WHEN 'activity_rings' THEN EXISTS (SELECT FROM fitness_activity_rings a
					WHERE a.user_id = r.user_id AND a.summary_date = r.day AND a.move_calories >= a.move_calories_goal
					  AND a.exercise_minutes >= a.exercise_minutes_goal AND a.stand_hours >= a.stand_hours_goal)
				WHEN 'sleep_goal' THEN EXISTS (SELECT FROM fitness_sleep s
					WHERE s.user_id = r.user_id AND s.sleep_date = r.day AND s.total_asleep_seconds >= 28800)
				ELSE false END),
			(SELECT count(*) FROM quest_days WHERE user_id <> $1 OR day >= $2::text::date - 1),
			(SELECT steps_count FROM fitness_activity_rings WHERE user_id = $1 AND summary_date = $2::text::date - 1)
	`, ownerID, today).Scan(&withoutSource, &outsideSeed, &realSteps)
	if err != nil || withoutSource != 0 || outsideSeed != 0 || realSteps != 42 {
		t.Fatalf("unsupported rewards = %d, out-of-scope days = %d, existing steps = %d, err = %v",
			withoutSource, outsideSeed, realSteps, err)
	}
	repeated, err := repo.seedHistory(ctx, ownerID)
	if err != nil || repeated != (debugSeedResult{}) {
		t.Fatalf("second seed result = %+v, err = %v", repeated, err)
	}
}
