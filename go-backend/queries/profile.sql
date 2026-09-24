-- name: GetProfileName :one
SELECT display_name FROM user_profiles WHERE user_id = $1;

-- name: SaveProfile :exec
INSERT INTO user_profiles (user_id, display_name)
VALUES ($1, $2)
ON CONFLICT (user_id)
DO UPDATE SET display_name = EXCLUDED.display_name, updated_at = NOW();

-- name: GetPasswordHash :one
SELECT password_hash FROM users WHERE id = $1;

-- name: UpdatePasswordHash :exec
UPDATE users SET password_hash = $1, updated_at = NOW() WHERE id = $2;
