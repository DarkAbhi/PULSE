CREATE TABLE financial_horizon_configs (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    base_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (base_amount >= 0),
    currency VARCHAR(10) NOT NULL DEFAULT '₹',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE financial_horizon_deductions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(64) NOT NULL DEFAULT 'bill',
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    due_day SMALLINT CHECK (due_day IS NULL OR (due_day >= 1 AND due_day <= 31)),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX horizon_deductions_user_idx ON financial_horizon_deductions (user_id, is_active, created_at);
