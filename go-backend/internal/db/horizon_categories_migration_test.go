//go:build integration

package db_test

import (
	"testing"

	"github.com/DarkAbhi/life-backend/internal/db"
	"github.com/DarkAbhi/life-backend/internal/testhelper"
)

func TestHorizonCategoriesPreserveExistingAssignments(t *testing.T) {
	t.Chdir("../..")
	database, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	var version int
	if err := database.QueryRow("SELECT version FROM schema_migrations").Scan(&version); err != nil {
		t.Fatal(err)
	}
	if version < 40 {
		t.Fatalf("expected migration 40 or later, got %d", version)
	}
	if err := db.RunMigrationSteps(dsn, 39-version); err != nil {
		t.Fatal(err)
	}

	var foodID, customID int64
	if err := database.QueryRow("SELECT id FROM financial_horizon_categories WHERE is_default AND name = 'Food & Dining'").Scan(&foodID); err != nil {
		t.Fatal(err)
	}
	if err := database.QueryRow("INSERT INTO financial_horizon_categories (user_id, name) VALUES (1, 'Personal label') RETURNING id").Scan(&customID); err != nil {
		t.Fatal(err)
	}
	if _, err := database.Exec(`
		INSERT INTO financial_horizon_transactions (user_id, name, amount, category_id)
		SELECT 1, 'Existing transaction', 5, id FROM financial_horizon_categories WHERE is_default AND name = 'Food & Dining';
		INSERT INTO financial_horizon_transactions (user_id, name, amount, category_name)
		VALUES (1, 'Label-only transaction', 5, 'Food & Dining'), (1, 'Uncategorized transaction', 5, 'Other');
		INSERT INTO financial_horizon_subscriptions (user_id, name, amount, category_id)
		SELECT 1, 'Existing subscription', 5, id FROM financial_horizon_categories WHERE is_default AND name = 'Food & Dining';
		INSERT INTO statement_category_rules (user_id, merchant_key, category_id)
		SELECT 1, 'existing-merchant', id FROM financial_horizon_categories WHERE is_default AND name = 'Food & Dining';
	`); err != nil {
		t.Fatal(err)
	}
	if err := db.RunMigrationSteps(dsn, 1); err != nil {
		t.Fatal(err)
	}
	var tx, labelOnly, subscription, rule int64
	var otherUncategorized bool
	if err := database.QueryRow(`
		SELECT (SELECT category_id FROM financial_horizon_transactions WHERE name = 'Existing transaction'),
		       (SELECT category_id FROM financial_horizon_transactions WHERE name = 'Label-only transaction'),
		       (SELECT category_id IS NULL FROM financial_horizon_transactions WHERE name = 'Uncategorized transaction'),
		       (SELECT category_id FROM financial_horizon_subscriptions WHERE name = 'Existing subscription'),
		       (SELECT category_id FROM statement_category_rules WHERE merchant_key = 'existing-merchant')
	`).Scan(&tx, &labelOnly, &otherUncategorized, &subscription, &rule); err != nil || tx != foodID || labelOnly != foodID ||
		!otherUncategorized || subscription != foodID || rule != foodID {
		t.Fatalf("category assignments: transaction=%d label-only=%d uncategorized=%t subscription=%d rule=%d want=%d, error=%v",
			tx, labelOnly, otherUncategorized, subscription, rule, foodID, err)
	}
	var name string
	var children int
	if err := database.QueryRow(`
		SELECT name, (SELECT count(*) FROM financial_horizon_categories WHERE parent_id = c.id)
		FROM financial_horizon_categories c WHERE id = $1
	`, foodID).Scan(&name, &children); err != nil || name != "Food and Drinks" || children != 14 {
		t.Fatalf("upgraded parent: name=%q children=%d error=%v", name, children, err)
	}
	var customPreserved bool
	if err := database.QueryRow(`
		SELECT EXISTS (SELECT 1 FROM financial_horizon_categories
		WHERE id = $1 AND user_id = 1 AND name = 'Personal label' AND parent_id IS NULL)
	`, customID).Scan(&customPreserved); err != nil || !customPreserved {
		t.Fatalf("custom category preserved=%t error=%v", customPreserved, err)
	}
}
