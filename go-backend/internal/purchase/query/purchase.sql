-- name: ListPurchases :many
SELECT id, name, price, url FROM next_month_purchases
WHERE user_id=$1 AND target_month=($2::text)::date
ORDER BY created_at DESC, id DESC;

-- name: CreatePurchase :one
INSERT INTO next_month_purchases (user_id,target_month,name,price,url)
VALUES ($1,($2::text)::date,$3,$4,$5)
RETURNING id,name,price,url;

-- name: DeletePurchase :execrows
DELETE FROM next_month_purchases
WHERE id=$1 AND user_id=$2 AND target_month=($3::text)::date;

-- name: ClearPurchases :exec
DELETE FROM next_month_purchases
WHERE user_id=$1 AND target_month=($2::text)::date;
