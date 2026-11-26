-- name: ReferencesAccessible :one
SELECT
    (sqlc.narg(category_id)::bigint IS NULL OR EXISTS (
        SELECT 1 FROM financial_horizon_categories r
        WHERE r.id = sqlc.narg(category_id) AND (r.user_id = sqlc.arg(user_id) OR (r.user_id IS NULL AND r.is_default))
    )) AND
    (sqlc.narg(budget_id)::bigint IS NULL OR EXISTS (
        SELECT 1 FROM financial_horizon_budgets r WHERE r.id = sqlc.narg(budget_id) AND r.user_id = sqlc.arg(user_id)
    )) AND
    (sqlc.narg(deduction_id)::bigint IS NULL OR EXISTS (
        SELECT 1 FROM financial_horizon_deductions r WHERE r.id = sqlc.narg(deduction_id) AND r.user_id = sqlc.arg(user_id)
    )) AND
    (sqlc.narg(subscription_id)::bigint IS NULL OR EXISTS (
        SELECT 1 FROM financial_horizon_subscriptions r WHERE r.id = sqlc.narg(subscription_id) AND r.user_id = sqlc.arg(user_id)
    )) AS accessible;
