-- name: ListNextMonthPurchases :many
SELECT id, name, price, url FROM next_month_purchases
WHERE user_id = $1 AND target_month = ($2::text)::date
ORDER BY created_at DESC, id DESC;

-- name: CreateNextMonthPurchase :one
INSERT INTO next_month_purchases (user_id, target_month, name, price, url)
VALUES ($1, ($2::text)::date, $3, $4, $5)
RETURNING id, name, price, url;

-- name: DeleteNextMonthPurchase :execrows
DELETE FROM next_month_purchases
WHERE id = $1 AND user_id = $2 AND target_month = ($3::text)::date;

-- name: ClearNextMonthPurchases :exec
DELETE FROM next_month_purchases WHERE user_id = $1 AND target_month = ($2::text)::date;
