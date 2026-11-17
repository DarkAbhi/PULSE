-- name: ListFinancialAccounts :many
SELECT * FROM financial_account_details
WHERE user_id = $1 AND (sqlc.arg(include_archived)::boolean OR archived_at IS NULL)
ORDER BY archived_at IS NOT NULL, lower(display_name), id;

-- name: GetFinancialAccount :one
SELECT * FROM financial_account_details WHERE id = $1 AND user_id = $2;

-- name: LockFinancialAccount :one
SELECT d.* FROM financial_account_details d JOIN financial_accounts a ON a.id = d.id
WHERE a.id = $1 AND a.user_id = $2 FOR UPDATE OF a;

-- name: CreateFinancialAccount :one
INSERT INTO financial_accounts (user_id, display_name, account_type)
VALUES ($1, $2, $3) RETURNING id;

-- name: UpdateFinancialAccount :one
UPDATE financial_accounts SET display_name = $3, account_type = $4, updated_at = CURRENT_TIMESTAMP
WHERE id = $1 AND user_id = $2 RETURNING id;

-- name: DeleteBankAccountDetails :exec
DELETE FROM bank_accounts WHERE account_id = $1;

-- name: DeleteCreditCardDetails :exec
DELETE FROM credit_cards WHERE account_id = $1;

-- name: SaveBankAccountDetails :exec
INSERT INTO bank_accounts (account_id, institution_name, account_number, holder_name, holder_email, bank_account_subtype, last_four)
VALUES ($1, $2, $3, $4, $5, $6, $7)
ON CONFLICT (account_id) DO UPDATE SET institution_name = EXCLUDED.institution_name,
account_number = EXCLUDED.account_number, holder_name = EXCLUDED.holder_name,
holder_email = EXCLUDED.holder_email, bank_account_subtype = EXCLUDED.bank_account_subtype, last_four = EXCLUDED.last_four;

-- name: SaveCreditCardDetails :exec
INSERT INTO credit_cards (account_id, institution_name, card_type, card_network, last_four, holder_name, credit_limit, statement_day)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
ON CONFLICT (account_id) DO UPDATE SET institution_name = EXCLUDED.institution_name,
card_type = EXCLUDED.card_type, card_network = EXCLUDED.card_network, last_four = EXCLUDED.last_four,
holder_name = EXCLUDED.holder_name, credit_limit = EXCLUDED.credit_limit, statement_day = EXCLUDED.statement_day;

-- name: FinancialAccountHasTransactions :one
SELECT (EXISTS (SELECT 1 FROM financial_horizon_transactions t WHERE t.account_id = $1 AND t.user_id = $2)
OR EXISTS (SELECT 1 FROM statement_import_batches b WHERE b.account_id = $1 AND b.user_id = $2 AND (b.status = 'committed' OR (b.status = 'ready' AND b.expires_at > CURRENT_TIMESTAMP))))::boolean AS used;

-- name: SetFinancialAccountArchived :one
UPDATE financial_accounts SET archived_at = CASE WHEN sqlc.arg(archived)::boolean THEN COALESCE(archived_at, CURRENT_TIMESTAMP) ELSE NULL END,
updated_at = CURRENT_TIMESTAMP WHERE id = $1 AND user_id = $2 RETURNING id;
