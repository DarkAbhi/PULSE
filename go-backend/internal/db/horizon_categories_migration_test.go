//go:build integration

package db_test

import (
	"testing"

	"github.com/DarkAbhi/life-backend/internal/db"
	"github.com/DarkAbhi/life-backend/internal/testhelper"
)

func TestHorizonCategoryReset(t *testing.T) {
	t.Chdir("../..")
	database, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()

	var version int
	if err := database.QueryRow("SELECT version FROM schema_migrations").Scan(&version); err != nil {
		t.Fatal(err)
	}
	if version < 41 {
		t.Fatalf("expected migration 41 or later, got %d", version)
	}
	if err := db.RunMigrationSteps(dsn, 39-version); err != nil {
		t.Fatal(err)
	}
	if _, err := database.Exec(`
		INSERT INTO financial_horizon_categories (user_id, name) VALUES (1, 'Personal label');
		INSERT INTO financial_horizon_transactions (user_id, name, amount, category_id)
		SELECT 1, 'Existing transaction', 5, id FROM financial_horizon_categories WHERE is_default AND name = 'Food & Dining';
		INSERT INTO financial_horizon_transactions (user_id, name, amount, category_name)
		VALUES (1, 'Label-only transaction', 7, 'Food & Dining');
		INSERT INTO financial_horizon_subscriptions (user_id, name, amount, category_id)
		SELECT 1, 'Existing subscription', 9, id FROM financial_horizon_categories WHERE name = 'Personal label' AND user_id = 1;
		INSERT INTO statement_category_rules (user_id, merchant_key, category_id)
		SELECT 1, 'existing-merchant', id FROM financial_horizon_categories WHERE is_default AND name = 'Food & Dining';

		WITH account AS (
			INSERT INTO financial_accounts (user_id, display_name, institution_name, account_type, currency)
			VALUES (1, 'Test account', 'Test institution', 'bank_account', 'INR') RETURNING id
		)
		INSERT INTO statement_import_batches
			(id, user_id, account_id, source_sha256, status, profile_id, currency, account_confirmed, expires_at)
		SELECT '00000000-0000-0000-0000-000000000041', 1, id, repeat('a', 64), 'ready', 'test', 'INR', true, now() + interval '1 day'
		FROM account;
		INSERT INTO statement_import_rows (batch_id, ordinal, source, preview)
		SELECT '00000000-0000-0000-0000-000000000041', 1, '{}',
			jsonb_build_object('category_id', id, 'category_changed', true, 'included', true)
		FROM financial_horizon_categories WHERE is_default AND name = 'Food & Dining';
	`); err != nil {
		t.Fatal(err)
	}
	if err := db.RunMigrationSteps(dsn, 1); err != nil {
		t.Fatal(err)
	}
	if _, err := database.Exec(`
		INSERT INTO financial_horizon_transactions (user_id, name, amount, category_id, category_name)
		SELECT 1, 'Subcategory transaction', 11, id, 'Food and Drinks / takeaway'
		FROM financial_horizon_categories WHERE parent_id IS NOT NULL AND name = 'takeaway';
	`); err != nil {
		t.Fatal(err)
	}
	if err := db.RunMigrationSteps(dsn, 1); err != nil {
		t.Fatal(err)
	}

	var roots, children, custom, oldDefaults int
	if err := database.QueryRow(`
		SELECT count(*) FILTER (WHERE parent_id IS NULL), count(*) FILTER (WHERE parent_id IS NOT NULL),
			count(*) FILTER (WHERE user_id IS NOT NULL),
			count(*) FILTER (WHERE name IN ('Health & Fitness', 'Investments & Savings'))
		FROM financial_horizon_categories
	`).Scan(&roots, &children, &custom, &oldDefaults); err != nil || roots != 32 || children != 231 || custom != 0 || oldDefaults != 0 {
		t.Fatalf("reset catalog: roots=%d children=%d custom=%d old=%d error=%v", roots, children, custom, oldDefaults, err)
	}
	var transactions, subscriptions, rules int
	if err := database.QueryRow(`
		SELECT
			(SELECT count(*) FROM financial_horizon_transactions WHERE category_id IS NULL AND category_name = 'Uncategorized'
				AND (name, amount) IN (('Existing transaction', 5), ('Label-only transaction', 7), ('Subcategory transaction', 11))),
			(SELECT count(*) FROM financial_horizon_subscriptions WHERE name = 'Existing subscription' AND amount = 9 AND category_id IS NULL),
			(SELECT count(*) FROM statement_category_rules)
	`).Scan(&transactions, &subscriptions, &rules); err != nil || transactions != 3 || subscriptions != 1 || rules != 0 {
		t.Fatalf("reset assignments: transactions=%d subscriptions=%d rules=%d error=%v", transactions, subscriptions, rules, err)
	}
	var revision int
	var included, cleared bool
	if err := database.QueryRow(`
		SELECT b.revision, (r.preview->>'included')::boolean,
			r.preview->'category_id' = 'null'::jsonb AND NOT (r.preview ? 'category_changed')
		FROM statement_import_batches b JOIN statement_import_rows r ON r.batch_id = b.id
		WHERE b.id = '00000000-0000-0000-0000-000000000041'
	`).Scan(&revision, &included, &cleared); err != nil || revision != 2 || !included || !cleared {
		t.Fatalf("reset statement review: revision=%d included=%t cleared=%t error=%v", revision, included, cleared, err)
	}
}
