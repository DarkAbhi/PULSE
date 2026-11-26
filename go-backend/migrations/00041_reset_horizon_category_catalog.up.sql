BEGIN;

ALTER TABLE financial_horizon_transactions ALTER COLUMN category_name SET DEFAULT 'Uncategorized';

UPDATE financial_horizon_transactions
SET category_id = NULL, category_name = DEFAULT
WHERE category_id IS NOT NULL OR category_name <> 'Uncategorized';

UPDATE financial_horizon_subscriptions SET category_id = NULL WHERE category_id IS NOT NULL;
DELETE FROM statement_category_rules;

-- Invalidate open reviews whose category selections changed during the reset.
WITH reset_rows AS (
    UPDATE statement_import_rows
    SET preview = jsonb_set(preview - 'category_changed', '{category_id}', 'null')
    WHERE preview->>'category_id' IS NOT NULL OR preview ? 'category_changed'
    RETURNING batch_id
)
UPDATE statement_import_batches
SET revision = revision + 1
WHERE status IN ('ready', 'unsupported') AND id IN (SELECT batch_id FROM reset_rows);

DELETE FROM financial_horizon_categories
WHERE user_id IS NOT NULL OR NOT is_default
   OR (parent_id IS NULL AND name IN ('Health & Fitness', 'Investments & Savings'));

COMMIT;
