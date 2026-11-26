-- name: ListDeductions :many
SELECT id, name, category, amount, due_day, is_active, budget_id
FROM financial_horizon_deductions WHERE user_id = $1
ORDER BY is_active DESC, created_at DESC, id DESC;

-- name: CreateDeduction :one
INSERT INTO financial_horizon_deductions (user_id, name, category, amount, due_day, is_active, budget_id)
VALUES ($1, $2, $3, $4, $5, $6, $7)
RETURNING id, name, category, amount, due_day, is_active, budget_id;

-- name: UpdateDeduction :one
UPDATE financial_horizon_deductions
SET name = $1, category = $2, amount = $3, due_day = $4, is_active = $5, budget_id = $6, updated_at = CURRENT_TIMESTAMP
WHERE id = $7 AND user_id = $8
RETURNING id, name, category, amount, due_day, is_active, budget_id;

-- name: DeleteDeduction :execrows
DELETE FROM financial_horizon_deductions WHERE id = $1 AND user_id = $2;
