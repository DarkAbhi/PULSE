//go:build integration

package db_test

import (
	"testing"

	"github.com/DarkAbhi/life-backend/internal/db"
	"github.com/DarkAbhi/life-backend/internal/testhelper"
)

func TestMealOnboardingUpgradeAndRollback(t *testing.T) {
	t.Chdir("../..")
	database, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	// Exercise migration 36 even when later migrations are added.
	for {
		var version int
		if err := database.QueryRow(`SELECT version FROM schema_migrations`).Scan(&version); err != nil {
			t.Fatal(err)
		}
		if version == 35 {
			break
		}
		if version < 35 {
			t.Fatalf("expected migration 35 or later, got %d", version)
		}
		if err := db.RollbackMigration(dsn); err != nil {
			t.Fatal(err)
		}
	}
	var id int64
	if err := database.QueryRow(`INSERT INTO users (username,password_hash) VALUES ('meal-migration','test') RETURNING id`).Scan(&id); err != nil {
		t.Fatal(err)
	}
	if _, err := database.Exec(`INSERT INTO meal_plans (user_id,date,name) VALUES ($1,'2026-11-05','Keep my meal')`, id); err != nil {
		t.Fatal(err)
	}
	for _, step := range []int{1, -1, 1} {
		if err := db.RunMigrationSteps(dsn, step); err != nil {
			t.Fatal(err)
		}
		for _, table := range []string{"meal_plan_onboarding", "meal_preferences", "meal_preference_times"} {
			var exists bool
			if err := database.QueryRow(`SELECT to_regclass($1) IS NOT NULL`, "public."+table).Scan(&exists); err != nil || exists != (step == 1) {
				t.Fatalf("%s after step %d: %v %v", table, step, exists, err)
			}
		}
		var count int
		if err := database.QueryRow(`SELECT count(*) FROM meal_plans WHERE user_id=$1 AND name='Keep my meal'`, id).Scan(&count); err != nil || count != 1 {
			t.Fatalf("existing data lost: %d %v", count, err)
		}
	}
	var count int
	if err := database.QueryRow(`SELECT count(*) FROM meal_plan_onboarding WHERE user_id=$1`, id).Scan(&count); err != nil || count != 0 {
		t.Fatalf("migration marked existing user: %d %v", count, err)
	}
}
