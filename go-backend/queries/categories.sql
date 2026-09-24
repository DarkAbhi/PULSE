-- name: ListCategories :many
SELECT id, name, icon, color, is_default, user_id
FROM financial_horizon_categories
WHERE user_id IS NULL OR user_id = $1
ORDER BY is_default DESC, name ASC;

-- name: CreateCategory :one
INSERT INTO financial_horizon_categories (user_id, name, icon, color, is_default)
VALUES ($1, $2, $3, $4, false)
RETURNING id, name, icon, color, is_default, user_id;

-- name: CountDefaultCategories :one
SELECT COUNT(*) FROM financial_horizon_categories WHERE is_default = true;

-- name: SeedDefaultCategory :exec
INSERT INTO financial_horizon_categories (name, icon, color, is_default)
VALUES ($1, $2, $3, true) ON CONFLICT DO NOTHING;
