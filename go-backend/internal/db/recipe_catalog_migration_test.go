//go:build integration

package db_test

import (
	"testing"

	"github.com/DarkAbhi/life-backend/internal/db"
	"github.com/DarkAbhi/life-backend/internal/testhelper"
)

func TestRecipeCatalogFreshInstallAndUpgrade(t *testing.T) {
	t.Chdir("../..")
	database, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()

	assertSeedCount := func(want int) {
		t.Helper()
		var count int
		if err := database.QueryRow("SELECT count(*) FROM catalog_recipes").Scan(&count); err != nil || count != want {
			t.Fatalf("recipe count = %d, want %d, err = %v", count, want, err)
		}
	}
	const templateID = "5e2d7928985ffca18ae8fb26"
	assertUnbrandedTemplate := func() {
		t.Helper()
		var author string
		if err := database.QueryRow(`SELECT credited_author FROM catalog_meal_templates WHERE id = $1`, templateID).Scan(&author); err != nil || author != "" {
			t.Fatalf("expected blank template credit, got %q, err = %v", author, err)
		}
	}
	assertSeedCount(749)
	assertUnbrandedTemplate()
	if err := db.RunMigrationSteps(dsn, -3); err != nil {
		t.Fatal(err)
	}
	if err := db.RunMigrationSteps(dsn, 1); err != nil {
		t.Fatal(err)
	}
	assertSeedCount(819)
	if _, err := database.Exec(`UPDATE catalog_meal_templates SET credited_author = 'Legacy source' WHERE id = $1`, templateID); err != nil {
		t.Fatal(err)
	}

	var userID int64
	if err := database.QueryRow(`INSERT INTO users (username, password_hash) VALUES ('catalog-migration','test') RETURNING id`).Scan(&userID); err != nil {
		t.Fatal(err)
	}
	const retiredID = "5d1f0781983ba767c7439c62"
	const retainedID = "63ae81694aa654d9b6ff0518"
	if _, err := database.Exec(`INSERT INTO meal_plans (user_id, date, name, recipe_id) VALUES
		($1, '2026-11-22', 'Kadai Paneer', $2),
		($1, '2026-11-22', 'Existing custom meal', NULL)`, userID, retiredID); err != nil {
		t.Fatal(err)
	}

	if err := db.RunMigrationSteps(dsn, 2); err != nil {
		t.Fatal(err)
	}
	assertSeedCount(749)
	assertUnbrandedTemplate()

	var id string
	if err := database.QueryRow(`SELECT id FROM catalog_recipes WHERE name = 'Kadai Paneer'`).Scan(&id); err != nil || id != retainedID {
		t.Fatalf("retained recipe = %q, want %q, err = %v", id, retainedID, err)
	}
	var preserved int
	if err := database.QueryRow(`SELECT count(*) FROM meal_plans WHERE user_id = $1 AND (
		(name = 'Kadai Paneer' AND recipe_id = $2) OR
		(name = 'Existing custom meal' AND recipe_id IS NULL)
	)`, userID, retainedID).Scan(&preserved); err != nil || preserved != 2 {
		t.Fatalf("preserved meals = %d, want 2, err = %v", preserved, err)
	}
}
