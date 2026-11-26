BEGIN;

-- A downgrade must not discard user-created categories or assignments.
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM financial_horizon_categories c
        WHERE c.parent_id IS NOT NULL AND (
            NOT c.is_default
            OR EXISTS (SELECT 1 FROM financial_horizon_transactions t WHERE t.category_id = c.id)
            OR EXISTS (SELECT 1 FROM financial_horizon_subscriptions s WHERE s.category_id = c.id)
            OR EXISTS (SELECT 1 FROM statement_category_rules r WHERE r.category_id = c.id)
            OR EXISTS (SELECT 1 FROM statement_import_rows r WHERE r.preview->>'category_id' = c.id::text)
        )
    ) THEN
        RAISE EXCEPTION 'Move subcategory assignments before rolling back Horizon subcategories';
    END IF;
    IF EXISTS (SELECT 1 FROM financial_horizon_transactions WHERE length(category_name) > 100) THEN
        RAISE EXCEPTION 'Shorten transaction category labels before rolling back Horizon subcategories';
    END IF;
END $$;

DELETE FROM financial_horizon_categories WHERE parent_id IS NOT NULL;
DROP INDEX horizon_categories_user_name_idx;
ALTER TABLE financial_horizon_categories
    DROP COLUMN parent_id,
    DROP COLUMN allows_subcategories;
CREATE UNIQUE INDEX horizon_categories_user_name_idx ON financial_horizon_categories (COALESCE(user_id, 0), LOWER(name));

UPDATE financial_horizon_categories AS c SET name = renamed.old_name
FROM (VALUES
    ('Food & Dining', 'Food and Drinks'),
    ('Bills & Utilities', 'Bill'),
    ('Housing', 'Home'),
    ('Transportation', 'Transport'),
    ('Subscriptions', 'Subscription'),
    ('Other', 'Miscellaneous')
) AS renamed(old_name, new_name)
WHERE c.is_default AND c.user_id IS NULL AND c.name = renamed.new_name;

ALTER TABLE financial_horizon_transactions ALTER COLUMN category_name TYPE VARCHAR(100);

COMMIT;
