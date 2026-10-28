-- name: GetDisplayName :one
SELECT display_name FROM user_profiles WHERE user_id=$1;

-- name: SaveDisplayName :exec
INSERT INTO user_profiles (user_id,display_name) VALUES ($1,$2)
ON CONFLICT (user_id) DO UPDATE SET display_name=EXCLUDED.display_name,updated_at=NOW();

-- name: GetTimeZone :one
SELECT timezone FROM user_profiles WHERE user_id=$1;

-- name: SaveTimeZone :exec
INSERT INTO user_profiles (user_id, display_name, timezone) VALUES ($1, '', $2)
ON CONFLICT (user_id) DO UPDATE SET timezone=EXCLUDED.timezone, updated_at=NOW();

-- name: GetCurrency :one
SELECT COALESCE((SELECT currency FROM user_profiles WHERE user_id=$1), 'GBP')::text AS currency;

-- name: SaveCurrency :exec
INSERT INTO user_profiles (user_id, display_name, currency) VALUES ($1, '', $2)
ON CONFLICT (user_id) DO UPDATE SET currency=EXCLUDED.currency, updated_at=NOW();
