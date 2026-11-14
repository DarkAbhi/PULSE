CREATE TABLE financial_horizon_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    billing_cycle VARCHAR(30) NOT NULL DEFAULT 'monthly' CHECK (billing_cycle IN ('monthly', 'yearly')),
    billing_day INT NULL CHECK (billing_day >= 1 AND billing_day <= 31),
    renewal_date TIMESTAMPTZ NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'paused', 'cancelled')),
    category_id BIGINT NULL REFERENCES financial_horizon_categories(id) ON DELETE SET NULL,
    budget_id BIGINT NULL REFERENCES financial_horizon_budgets(id) ON DELETE SET NULL,
    deduction_id BIGINT NULL REFERENCES financial_horizon_deductions(id) ON DELETE SET NULL,
    notes TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX horizon_subscriptions_user_idx ON financial_horizon_subscriptions (user_id, status, created_at);

ALTER TABLE financial_horizon_transactions
ADD COLUMN subscription_id BIGINT NULL REFERENCES financial_horizon_subscriptions(id) ON DELETE SET NULL;

CREATE INDEX horizon_transactions_subscription_idx ON financial_horizon_transactions (subscription_id);
