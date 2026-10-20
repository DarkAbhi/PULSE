CREATE TABLE financial_horizon_categories (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(50) NOT NULL DEFAULT 'tag',
    color VARCHAR(30) NOT NULL DEFAULT '#64748b',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX horizon_categories_user_name_idx ON financial_horizon_categories (COALESCE(user_id, 0), LOWER(name));

INSERT INTO financial_horizon_categories (name, icon, color, is_default) VALUES
    ('Food & Dining', 'utensils', '#f97316', true),
    ('Bills & Utilities', 'receipt', '#ef4444', true),
    ('Housing', 'home', '#8b5cf6', true),
    ('Transportation', 'car', '#3b82f6', true),
    ('Shopping', 'shopping-bag', '#ec4899', true),
    ('Entertainment', 'film', '#14b8a6', true),
    ('Health & Fitness', 'heart-pulse', '#06b6d4', true),
    ('Investments & Savings', 'trending-up', '#22c55e', true),
    ('Subscriptions', 'credit-card', '#6366f1', true),
    ('Other', 'tag', '#64748b', true)
ON CONFLICT DO NOTHING;

CREATE TABLE financial_horizon_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    transaction_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    category_id BIGINT NULL REFERENCES financial_horizon_categories(id) ON DELETE SET NULL,
    category_name VARCHAR(100) NOT NULL DEFAULT 'Other',
    budget_id BIGINT NULL REFERENCES financial_horizon_budgets(id) ON DELETE SET NULL,
    notes TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX horizon_transactions_user_date_idx ON financial_horizon_transactions (user_id, transaction_date DESC);
CREATE INDEX horizon_transactions_category_idx ON financial_horizon_transactions (category_id);
CREATE INDEX horizon_transactions_budget_idx ON financial_horizon_transactions (budget_id);
