-- name: GetHorizonConfig :one
SELECT base_amount, currency FROM financial_horizon_configs WHERE user_id = $1;

-- name: UpsertHorizonConfig :exec
INSERT INTO financial_horizon_configs (user_id, base_amount, currency, updated_at)
VALUES ($1, $2, $3, CURRENT_TIMESTAMP)
ON CONFLICT (user_id) DO UPDATE
SET base_amount = EXCLUDED.base_amount, currency = EXCLUDED.currency, updated_at = CURRENT_TIMESTAMP;
