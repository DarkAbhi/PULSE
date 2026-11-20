CREATE TABLE financial_horizon_budgets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    allocated_amount NUMERIC(12,2) NOT NULL CHECK (allocated_amount >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX horizon_budgets_user_idx ON financial_horizon_budgets (user_id, created_at);

ALTER TABLE financial_horizon_deductions
ADD COLUMN budget_id BIGINT NULL REFERENCES financial_horizon_budgets(id) ON DELETE SET NULL;

CREATE INDEX horizon_deductions_budget_idx ON financial_horizon_deductions (budget_id);
