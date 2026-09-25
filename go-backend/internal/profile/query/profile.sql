-- name: GetDisplayName :one
SELECT display_name FROM user_profiles WHERE user_id=$1;

-- name: SaveDisplayName :exec
INSERT INTO user_profiles (user_id,display_name) VALUES ($1,$2)
ON CONFLICT (user_id) DO UPDATE SET display_name=EXCLUDED.display_name,updated_at=NOW();
