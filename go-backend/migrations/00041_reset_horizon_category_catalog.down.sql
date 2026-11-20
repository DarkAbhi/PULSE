BEGIN;

ALTER TABLE financial_horizon_transactions ALTER COLUMN category_name SET DEFAULT 'Other';

-- Restore the previous default catalog. Cleared assignments and custom categories require a backup.
INSERT INTO financial_horizon_categories (name, icon, color, is_default, allows_subcategories)
VALUES ('Health & Fitness', 'heart-pulse', '#06b6d4', TRUE, FALSE),
       ('Investments & Savings', 'trending-up', '#22c55e', TRUE, FALSE)
ON CONFLICT DO NOTHING;

COMMIT;
