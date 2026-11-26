//go:build integration

package quests

import (
	"context"
	"testing"
	"time"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/source/file"
	"github.com/jackc/pgx/v5/pgxpool"
)

func TestMealQuestUpgradeFromMigration52(t *testing.T) {
	db, dsn, stop := testhelper.StartPostgresWithDSN(t)
	t.Cleanup(stop)
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
	exec := func(query string) {
		t.Helper()
		if _, err := db.Exec(query); err != nil {
			t.Fatal(err)
		}
	}
	check := func(wantEnabled bool) {
		t.Helper()
		var enabled bool
		var triggerFunction string
		err := db.QueryRow(`
			SELECT s.daily_meals, p.proname
			FROM user_quest_settings s JOIN users u ON u.id = s.user_id
			CROSS JOIN pg_trigger t JOIN pg_proc p ON p.oid = t.tgfoid
			WHERE u.username = 'legacy-meal-quest'
			  AND t.tgrelid = 'meal_plans'::regclass AND t.tgname = 'quest_meals'
		`).Scan(&enabled, &triggerFunction)
		if err != nil {
			t.Fatal(err)
		}
		if enabled != wantEnabled || triggerFunction != "quest_capture_progress" {
			t.Fatalf("upgraded settings and trigger = (%t, %s), want (%t, quest_capture_progress)",
				enabled, triggerFunction, wantEnabled)
		}
	}

	// Start at the originally published form of 52.
	if err := migrations.Migrate(52); err != nil {
		t.Fatal(err)
	}
	exec(`
		WITH legacy_user AS (
			INSERT INTO users (username, password_hash) VALUES ('legacy-meal-quest', 'test') RETURNING id
		)
		INSERT INTO user_quest_settings (user_id, effective_date, gym_workout, activity_rings, meal_target)
		SELECT id, CURRENT_DATE, false, false, 3 FROM legacy_user
	`)
	step(1)
	check(true)

	// The revised form of 52 already had the column. Keep an explicit opt-out on upgrade.
	step(-1)
	exec(`
		ALTER TABLE user_quest_settings ADD COLUMN daily_meals BOOLEAN NOT NULL DEFAULT false;
		DROP TRIGGER quest_meals ON meal_plans;
		DROP FUNCTION quest_capture_meal_plan();
		CREATE TRIGGER quest_meals AFTER INSERT OR UPDATE OF date, is_consumed OR DELETE ON meal_plans
		FOR EACH ROW EXECUTE FUNCTION quest_capture_progress()
	`)
	step(1)
	check(false)
}

func TestMealPlanDrivesDailyQuest(t *testing.T) {
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
	if err := db.QueryRow(`
		INSERT INTO users (username, password_hash) VALUES ('meal-quests', 'test') RETURNING id
	`).Scan(&userID); err != nil {
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
	initial := overview()
	if initial.Configured || initial.Settings.DailyMeals || len(initial.TodayQuests) != 0 {
		t.Fatalf("expected no quests before planning meals: %+v", initial)
	}

	checkMeals := func(target, progress int, completed bool) {
		t.Helper()
		var gotTarget, gotProgress int
		var gotCompleted bool
		err := db.QueryRow(`
			SELECT COALESCE(max(q.target), 0), COALESCE(max(q.progress), 0), count(r.quest_id) > 0
			FROM daily_quests q LEFT JOIN quest_reward_ledger r USING (user_id, day, quest_id)
			WHERE q.user_id = $1 AND q.day = $2::text::date AND q.quest_id = 'daily_meals'
		`, userID, initial.Today).Scan(&gotTarget, &gotProgress, &gotCompleted)
		if err != nil {
			t.Fatal(err)
		}
		if gotTarget != target || gotProgress != progress || gotCompleted != completed {
			t.Fatalf("meal quest = %d/%d (complete: %t), want %d/%d (complete: %t)",
				gotProgress, gotTarget, gotCompleted, progress, target, completed)
		}
	}

	exec(`
		INSERT INTO meal_plans (user_id, date, name, is_consumed) VALUES
			($1, $2::text::date, 'Breakfast', true), ($1, $2::text::date, 'Lunch', false),
			($1, $2::text::date - 1, 'Yesterday', false)
	`, userID, initial.Today)
	checkMeals(0, 0, false)
	if got := overview(); got.Configured || got.Settings.DailyMeals || len(got.TodayQuests) != 0 {
		t.Fatalf("expected the meal quest to stay off without opt-in: %+v", got)
	}

	stepTarget := int32(8000)
	settings := Settings{StepTarget: &stepTarget, DailyMeals: true}
	if _, err := repo.SaveSettings(ctx, userID, settings); err != nil {
		t.Fatal(err)
	}
	checkMeals(2, 1, false)
	if got := overview(); !got.Configured || !got.Settings.DailyMeals || len(got.TodayQuests) != 2 ||
		got.TodayQuests[0].ID != "daily_steps" || got.TodayQuests[1].ID != "daily_meals" {
		t.Fatalf("expected steps and the opted-in meal quest: %+v", got)
	}

	exec("UPDATE meal_plans SET is_consumed = true WHERE user_id = $1 AND name = 'Lunch'", userID)
	checkMeals(2, 2, true)

	exec("INSERT INTO meal_plans (user_id, date, name) VALUES ($1, $2::text::date, 'Dinner')", userID, initial.Today)
	checkMeals(3, 2, false)
	exec("UPDATE meal_plans SET date = date + 1 WHERE user_id = $1 AND name = 'Dinner'", userID)
	checkMeals(2, 2, true)

	exec("UPDATE meal_plans SET is_consumed = false WHERE user_id = $1 AND name = 'Lunch'", userID)
	checkMeals(2, 1, false)
	exec("DELETE FROM meal_plans WHERE user_id = $1 AND date = $2::text::date", userID, initial.Today)
	checkMeals(0, 0, false)

	exec("INSERT INTO meal_plans (user_id, date, name, is_consumed) VALUES ($1, $2::text::date, 'Snack', true)", userID, initial.Today)
	checkMeals(1, 1, true)

	settings.DailyMeals = false
	effective, err := repo.SaveSettings(ctx, userID, settings)
	if err != nil {
		t.Fatal(err)
	}
	if effective != initial.Today {
		t.Fatalf("opt-out takes effect on %s, want %s", effective, initial.Today)
	}
	checkMeals(0, 0, false)
	if got := overview(); got.Settings.DailyMeals || len(got.TodayQuests) != 1 || got.TodayQuests[0].ID != "daily_steps" ||
		got.Progress.TodayXP != 0 || got.Progress.LifetimeXP != 0 || got.Progress.Hearts != 0 {
		t.Fatalf("expected only steps and no meal rewards after opting out: %+v", got)
	}
	exec("INSERT INTO meal_plans (user_id, date, name, is_consumed) VALUES ($1, $2::text::date, 'Dinner', true)", userID, initial.Today)
	checkMeals(0, 0, false)

	settings.DailyMeals = true
	if _, err := repo.SaveSettings(ctx, userID, settings); err != nil {
		t.Fatal(err)
	}
	checkMeals(2, 2, true)
	if got := overview(); got.Progress.TodayXP != 20 || got.Progress.Hearts != 1 {
		t.Fatalf("expected a single meal reward after opting back in: %+v", got.Progress)
	}
}

func TestQuestGoalChangesReconcileToday(t *testing.T) {
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
	var today string
	if err := db.QueryRow(`
		INSERT INTO users (username, password_hash) VALUES ('test-immediate-goals', 'test')
		RETURNING id, (clock_timestamp() AT TIME ZONE 'UTC')::date::text
	`).Scan(&userID, &today); err != nil {
		t.Fatal(err)
	}
	exec := func(query string, args ...any) {
		t.Helper()
		if _, err := db.Exec(query, args...); err != nil {
			t.Fatal(err)
		}
	}

	// Preserve a completed day and supersede the queued change created by the old flow.
	exec(`
		INSERT INTO user_quest_settings (user_id, effective_date, gym_workout, activity_rings, step_target)
		VALUES ($1, $2::text::date - 1, false, false, 4000), ($1, $2::text::date + 1, false, false, 9000)
	`, userID, today)
	exec("INSERT INTO quest_days (user_id, day, timezone) VALUES ($1, $2::text::date - 1, 'UTC')", userID, today)
	exec(`
		INSERT INTO daily_quests (user_id, day, quest_id, target, progress, xp, hearts)
		VALUES ($1, $2::text::date - 1, 'daily_steps', 4000, 4000, 20, 1)
	`, userID, today)
	exec(`
		INSERT INTO quest_reward_ledger (user_id, day, quest_id, xp, hearts)
		VALUES ($1, $2::text::date - 1, 'daily_steps', 20, 1)
	`, userID, today)
	exec(`
		INSERT INTO fitness_activity_rings (user_id, summary_date, steps_count)
		VALUES ($1, $2::text::date, 5000)
	`, userID, today)

	save := func(settings Settings) {
		t.Helper()
		effective, err := repo.SaveSettings(ctx, userID, settings)
		if err != nil || effective != today {
			t.Fatalf("save = (%q, %v), want (%q, nil)", effective, err, today)
		}
	}
	check := func(target, progress int, complete bool) {
		t.Helper()
		// Check storage before Overview has a chance to reconcile it.
		var gotTarget, gotProgress int
		var gotComplete bool
		err := db.QueryRow(`
			SELECT COALESCE(max(q.target), 0), COALESCE(max(q.progress), 0), count(r.quest_id) > 0
			FROM daily_quests q LEFT JOIN quest_reward_ledger r USING (user_id, day, quest_id)
			WHERE q.user_id = $1 AND q.day = $2::text::date AND q.quest_id = 'daily_steps'
		`, userID, today).Scan(&gotTarget, &gotProgress, &gotComplete)
		if err != nil || gotTarget != target || gotProgress != progress || gotComplete != complete {
			t.Fatalf("stored steps = %d/%d (complete: %t), err = %v, want %d/%d (complete: %t)",
				gotProgress, gotTarget, gotComplete, err, progress, target, complete)
		}
		overview, err := repo.Overview(ctx, userID, time.Time{})
		if err != nil {
			t.Fatal(err)
		}
		todayXP, todayHearts := 0, 0
		if complete {
			todayXP, todayHearts = 20, 1
		}
		p := overview.Progress
		if p.TodayXP != todayXP || p.LifetimeXP != todayXP+20 || p.Hearts != todayHearts+1 {
			t.Fatalf("unexpected rewards with today's steps %d/%d: %+v", progress, target, p)
		}
		if target == 0 {
			if overview.Settings.StepTarget != nil || len(overview.TodayQuests) != 0 {
				t.Fatalf("expected steps disabled: %+v", overview)
			}
		} else if overview.Settings.StepTarget == nil || int(*overview.Settings.StepTarget) != target ||
			len(overview.TodayQuests) != 1 || overview.TodayQuests[0].Completed != complete {
			t.Fatalf("unexpected current quest or settings: %+v", overview)
		}
	}

	check(4000, 4000, true)
	higher, lower := int32(6000), int32(4500)
	save(Settings{StepTarget: &higher})
	check(6000, 5000, false)
	save(Settings{StepTarget: &lower})
	check(4500, 4500, true)
	save(Settings{StepTarget: &lower})
	check(4500, 4500, true)
	save(Settings{})
	check(0, 0, false)
	save(Settings{StepTarget: &lower})
	check(4500, 4500, true)
}
