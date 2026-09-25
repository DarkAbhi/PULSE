-- name: ListBudgets :many
SELECT id, name, allocated_amount FROM financial_horizon_budgets
WHERE user_id = $1 ORDER BY created_at ASC, id ASC;

-- name: CreateBudget :one
INSERT INTO financial_horizon_budgets (user_id, name, allocated_amount)
VALUES ($1, $2, $3) RETURNING id, name, allocated_amount;

-- name: UpdateBudget :one
UPDATE financial_horizon_budgets
SET name = $1, allocated_amount = $2, updated_at = CURRENT_TIMESTAMP
WHERE id = $3 AND user_id = $4
RETURNING id, name, allocated_amount;

-- name: GetBudgetUsedAmount :one
SELECT COALESCE(SUM(amount), 0)::double precision FROM financial_horizon_deductions
WHERE budget_id = $1 AND user_id = $2 AND is_active = true;

-- name: DeleteBudget :execrows
DELETE FROM financial_horizon_budgets WHERE id = $1 AND user_id = $2;
