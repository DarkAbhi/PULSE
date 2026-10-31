-- name: ListSubscriptions :many
SELECT s.id, s.name, s.amount, s.billing_cycle, s.billing_day, s.renewal_date, s.status,
       s.category_id, c.name AS category_name, s.budget_id, b.name AS budget_name, s.deduction_id, s.notes, s.created_at, s.updated_at,
       COALESCE(COUNT(t.id), 0)::bigint AS linked_count,
       COALESCE(SUM(t.amount), 0)::double precision AS total_spent
FROM financial_horizon_subscriptions s
LEFT JOIN financial_horizon_categories c ON s.category_id = c.id
LEFT JOIN financial_horizon_budgets b ON s.budget_id = b.id
LEFT JOIN financial_horizon_transactions t ON t.subscription_id = s.id
WHERE s.user_id = $1
GROUP BY s.id, c.name, b.name
ORDER BY CASE WHEN s.status = 'active' THEN 1 WHEN s.status = 'paused' THEN 2 ELSE 3 END, s.created_at DESC, s.id DESC;

-- name: CreateSubscription :one
INSERT INTO financial_horizon_subscriptions (user_id, name, amount, billing_cycle, billing_day, renewal_date, status, category_id, budget_id, deduction_id, notes)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
RETURNING id, name, amount, billing_cycle, billing_day, renewal_date, status, category_id, budget_id, deduction_id, notes, created_at, updated_at;

-- name: UpdateSubscription :one
UPDATE financial_horizon_subscriptions
SET name = $1, amount = $2, billing_cycle = $3, billing_day = $4, renewal_date = $5, status = $6,
    category_id = $7, budget_id = $8, deduction_id = $9, notes = $10, updated_at = CURRENT_TIMESTAMP
WHERE id = $11 AND user_id = $12
RETURNING id, name, amount, billing_cycle, billing_day, renewal_date, status, category_id, budget_id, deduction_id, notes, created_at, updated_at;

-- name: GetCategoryName :one
SELECT name FROM financial_horizon_categories WHERE id = $1;

-- name: GetBudgetName :one
SELECT name FROM financial_horizon_budgets WHERE id = $1 AND user_id = $2;

-- name: GetSubscriptionTransactionStats :one
SELECT COALESCE(COUNT(id), 0)::bigint AS linked_count, COALESCE(SUM(amount), 0)::double precision AS total_spent
FROM financial_horizon_transactions WHERE subscription_id = $1 AND user_id = $2;

-- name: DeleteSubscription :execrows
DELETE FROM financial_horizon_subscriptions WHERE id = $1 AND user_id = $2;

-- name: ListSubscriptionTransactions :many
SELECT t.id, t.name, t.amount, t.type, t.transaction_date, t.category_id,
       COALESCE(c.name, t.category_name)::text AS category_name, t.budget_id, b.name AS budget_name,
       t.subscription_id, s.name AS subscription_name, t.notes, t.created_at
FROM financial_horizon_transactions t
LEFT JOIN financial_horizon_categories c ON t.category_id = c.id
LEFT JOIN financial_horizon_budgets b ON t.budget_id = b.id
LEFT JOIN financial_horizon_subscriptions s ON t.subscription_id = s.id
WHERE t.user_id = $1 AND t.subscription_id = $2
ORDER BY t.transaction_date DESC, t.id DESC;
