-- name: ListRecentTransactions :many
SELECT t.id, t.name, t.amount, t.type, t.transaction_date, t.category_id,
       COALESCE(c.name, t.category_name)::text AS category_name, t.budget_id, b.name AS budget_name,
       t.subscription_id, s.name AS subscription_name, t.notes, t.created_at
FROM financial_horizon_transactions t
LEFT JOIN financial_horizon_categories c ON t.category_id = c.id
LEFT JOIN financial_horizon_budgets b ON t.budget_id = b.id
LEFT JOIN financial_horizon_subscriptions s ON t.subscription_id = s.id
WHERE t.user_id = $1
ORDER BY t.transaction_date DESC, t.id DESC LIMIT $2;

-- name: ListRecentTransactionsLegacy :many
SELECT t.id, t.name, t.amount, t.type, t.transaction_date, t.category_id,
       COALESCE(c.name, t.category_name)::text AS category_name, t.budget_id, b.name AS budget_name,
       NULL::bigint AS subscription_id, NULL::text AS subscription_name, t.notes, t.created_at
FROM financial_horizon_transactions t
LEFT JOIN financial_horizon_categories c ON t.category_id = c.id
LEFT JOIN financial_horizon_budgets b ON t.budget_id = b.id
WHERE t.user_id = $1
ORDER BY t.transaction_date DESC, t.id DESC LIMIT $2;

-- name: CountFilteredTransactions :one
SELECT COUNT(*) FROM financial_horizon_transactions t
WHERE t.user_id = $1
  AND ($2::text = '' OR t.type = $2)
  AND ($3::text = '' OR LOWER(t.name) LIKE $3 OR LOWER(t.notes) LIKE $3);

-- name: ListFilteredTransactions :many
SELECT t.id, t.name, t.amount, t.type, t.transaction_date, t.category_id,
       COALESCE(c.name, t.category_name)::text AS category_name, t.budget_id, b.name AS budget_name,
       t.subscription_id, s.name AS subscription_name, t.notes, t.created_at
FROM financial_horizon_transactions t
LEFT JOIN financial_horizon_categories c ON t.category_id = c.id
LEFT JOIN financial_horizon_budgets b ON t.budget_id = b.id
LEFT JOIN financial_horizon_subscriptions s ON t.subscription_id = s.id
WHERE t.user_id = $1
  AND ($2::text = '' OR t.type = $2)
  AND ($3::text = '' OR LOWER(t.name) LIKE $3 OR LOWER(t.notes) LIKE $3)
ORDER BY t.transaction_date DESC, t.id DESC LIMIT $4 OFFSET $5;

-- name: ListFilteredTransactionsLegacy :many
SELECT t.id, t.name, t.amount, t.type, t.transaction_date, t.category_id,
       COALESCE(c.name, t.category_name)::text AS category_name, t.budget_id, b.name AS budget_name,
       NULL::bigint AS subscription_id, NULL::text AS subscription_name, t.notes, t.created_at
FROM financial_horizon_transactions t
LEFT JOIN financial_horizon_categories c ON t.category_id = c.id
LEFT JOIN financial_horizon_budgets b ON t.budget_id = b.id
WHERE t.user_id = $1
  AND ($2::text = '' OR t.type = $2)
  AND ($3::text = '' OR LOWER(t.name) LIKE $3 OR LOWER(t.notes) LIKE $3)
ORDER BY t.transaction_date DESC, t.id DESC LIMIT $4 OFFSET $5;

-- name: GetUserCategoryName :one
SELECT name FROM financial_horizon_categories WHERE id = $1 AND (user_id IS NULL OR user_id = $2);

-- name: CreateTransaction :one
INSERT INTO financial_horizon_transactions (user_id, name, amount, type, transaction_date, category_id, category_name, budget_id, subscription_id, notes)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
RETURNING id, name, amount, type, transaction_date, category_id, category_name, budget_id, subscription_id, notes, created_at;

-- name: GetSubscriptionName :one
SELECT name FROM financial_horizon_subscriptions WHERE id = $1 AND user_id = $2;

-- name: UpdateTransaction :one
UPDATE financial_horizon_transactions
SET name = $1, amount = $2, type = $3, transaction_date = $4, category_id = $5, category_name = $6,
    budget_id = $7, subscription_id = $8, notes = $9, updated_at = CURRENT_TIMESTAMP
WHERE id = $10 AND user_id = $11
RETURNING id, name, amount, type, transaction_date, category_id, category_name, budget_id, subscription_id, notes, created_at;

-- name: DeleteTransaction :execrows
DELETE FROM financial_horizon_transactions WHERE id = $1 AND user_id = $2;
