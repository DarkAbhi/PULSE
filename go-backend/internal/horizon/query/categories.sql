-- name: ListCategories :many
SELECT id, name, icon, color, is_default, user_id, parent_id, allows_subcategories
FROM financial_horizon_categories
WHERE (user_id IS NULL AND is_default) OR user_id = $1
ORDER BY is_default DESC, name ASC;

-- name: CreateCategory :one
INSERT INTO financial_horizon_categories (user_id, name, icon, color, is_default, parent_id, allows_subcategories)
SELECT sqlc.narg(user_id)::bigint, sqlc.arg(name)::varchar, sqlc.arg(icon)::varchar, sqlc.arg(color)::varchar,
       false, sqlc.narg(parent_id)::bigint, sqlc.narg(parent_id)::bigint IS NULL
WHERE sqlc.narg(parent_id)::bigint IS NULL OR EXISTS (
    SELECT 1 FROM financial_horizon_categories p
    WHERE p.id = sqlc.narg(parent_id) AND p.parent_id IS NULL AND p.allows_subcategories
      AND (p.user_id = sqlc.narg(user_id) OR (p.user_id IS NULL AND p.is_default))
)
RETURNING id, name, icon, color, is_default, user_id, parent_id, allows_subcategories;
