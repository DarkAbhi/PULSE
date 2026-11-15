-- name: ListFinancialAccounts :many
SELECT * FROM financial_accounts
WHERE user_id = $1 AND (sqlc.arg(include_archived)::boolean OR archived_at IS NULL)
ORDER BY archived_at IS NOT NULL, lower(display_name), id;

-- name: GetFinancialAccount :one
SELECT * FROM financial_accounts WHERE id = $1 AND user_id = $2;

-- name: LockFinancialAccount :one
SELECT * FROM financial_accounts WHERE id = $1 AND user_id = $2 FOR UPDATE;

-- name: CreateFinancialAccount :one
INSERT INTO financial_accounts (user_id, display_name, institution_name, account_type, bank_account_subtype, currency, country_code, last_four)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING *;

-- name: UpdateFinancialAccount :one
UPDATE financial_accounts SET display_name = $3, institution_name = $4, account_type = $5,
bank_account_subtype = $6, currency = $7, country_code = $8, last_four = $9, updated_at = CURRENT_TIMESTAMP
WHERE id = $1 AND user_id = $2 RETURNING *;

-- name: FinancialAccountHasTransactions :one
SELECT (EXISTS (SELECT 1 FROM financial_horizon_transactions t WHERE t.account_id = $1 AND t.user_id = $2)
OR EXISTS (SELECT 1 FROM statement_import_batches b WHERE b.account_id = $1 AND b.user_id = $2 AND (b.status = 'committed' OR (b.status = 'ready' AND b.expires_at > CURRENT_TIMESTAMP))))::boolean AS used;

-- name: SetFinancialAccountArchived :one
UPDATE financial_accounts SET archived_at = CASE WHEN sqlc.arg(archived)::boolean THEN COALESCE(archived_at, CURRENT_TIMESTAMP) ELSE NULL END,
updated_at = CURRENT_TIMESTAMP WHERE id = $1 AND user_id = $2 RETURNING *;
