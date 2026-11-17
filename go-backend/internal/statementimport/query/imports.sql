-- name: CreateBatch :one
INSERT INTO statement_import_batches (
    id, user_id, account_id, source_sha256, status, profile_id, currency, ocr_used, reason_code, account_confirmed,
    parsed_count, duplicate_count, opening_minor, closing_minor, expires_at
) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15) RETURNING *;

-- name: FindSourceBatch :one
SELECT b.* FROM statement_import_batches b
WHERE b.user_id = $1 AND b.account_id = $2 AND b.source_sha256 = $3 AND (
    (b.status = 'ready' AND b.expires_at > CURRENT_TIMESTAMP)
    OR (b.status = 'committed' AND (b.imported_count = 0 OR EXISTS (
        SELECT 1 FROM financial_horizon_transactions t WHERE t.import_batch_id = b.id
    )))
)
ORDER BY (b.status = 'committed') DESC, b.created_at DESC LIMIT 1;

-- name: GetBatch :one
SELECT * FROM statement_import_batches WHERE id = $1 AND user_id = $2;

-- name: LockBatch :one
SELECT * FROM statement_import_batches WHERE id = $1 AND user_id = $2 FOR UPDATE;

-- name: AddRow :exec
INSERT INTO statement_import_rows (batch_id, ordinal, source, preview) VALUES ($1,$2,$3,$4);

-- name: ListRows :many
SELECT * FROM statement_import_rows WHERE batch_id = $1 ORDER BY ordinal;

-- name: ListPage :many
SELECT * FROM statement_import_rows WHERE batch_id = $1 ORDER BY ordinal LIMIT $2 OFFSET $3;

-- name: UpdateRow :exec
UPDATE statement_import_rows SET preview = $3 WHERE batch_id = $1 AND ordinal = $2;

-- name: UpdateBatch :one
UPDATE statement_import_batches SET revision = revision + 1, account_confirmed = $2, excluded_count = $3
WHERE id = $1 RETURNING *;

-- name: CommitBatch :one
UPDATE statement_import_batches SET status = 'committed', committed_at = CURRENT_TIMESTAMP,
    imported_count = $2, duplicate_count = $3, excluded_count = $4
WHERE id = $1 RETURNING *;

-- name: CancelBatch :exec
UPDATE statement_import_batches SET status = 'cancelled' WHERE id = $1;

-- name: RemoveRows :exec
DELETE FROM statement_import_rows WHERE batch_id = $1;

-- name: AddStructure :exec
INSERT INTO statement_import_structures (batch_id, document) VALUES ($1,$2);

-- name: GetStructure :one
SELECT document FROM statement_import_structures WHERE batch_id = $1;

-- name: RemoveStructure :exec
DELETE FROM statement_import_structures WHERE batch_id = $1;

-- name: ExpireBatches :exec
WITH expired AS (
    UPDATE statement_import_batches SET status = 'expired'
    WHERE expires_at <= CURRENT_TIMESTAMP AND status IN ('ready', 'unsupported')
    RETURNING id
), removed_rows AS (
    DELETE FROM statement_import_rows WHERE batch_id IN (SELECT id FROM expired)
)
DELETE FROM statement_import_structures WHERE batch_id IN (SELECT id FROM expired);

-- name: ListFingerprints :many
SELECT d.fingerprint, d.occurrence FROM statement_import_dedupe d
JOIN statement_import_batches b ON b.id = d.batch_id
WHERE d.user_id = $1 AND d.account_id = $2 AND d.fingerprint = ANY(sqlc.arg(hashes)::text[])
    AND (b.source_sha256 <> sqlc.arg(source_sha256) OR EXISTS (
        SELECT 1 FROM financial_horizon_transactions t WHERE t.import_batch_id = d.batch_id
    ));

-- name: RecordFingerprint :exec
INSERT INTO statement_import_dedupe (user_id,account_id,fingerprint,occurrence,batch_id,ordinal) VALUES ($1,$2,$3,$4,$5,$6)
ON CONFLICT (user_id, account_id, fingerprint, occurrence) DO UPDATE
SET batch_id = EXCLUDED.batch_id, ordinal = EXCLUDED.ordinal;

-- name: ImportTransaction :exec
INSERT INTO financial_horizon_transactions (
    user_id, account_id, name, amount, type, transaction_date, category_id, category_name,
    source, import_batch_id, import_row_ordinal, dedupe_hash
) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,'statement',$9,$10,$11);

-- name: ListMerchantCategories :many
SELECT merchant_key, category_id FROM statement_category_rules
WHERE user_id = sqlc.arg(user_id) AND merchant_key = ANY(sqlc.arg(merchant_keys)::text[]);

-- name: LearnCategory :exec
INSERT INTO statement_category_rules (user_id, merchant_key, category_id) VALUES ($1,$2,$3)
ON CONFLICT (user_id, merchant_key) DO UPDATE SET category_id = EXCLUDED.category_id;

-- name: PossibleManualMatches :many
SELECT DISTINCT candidates.ordinal::integer
FROM ROWS FROM (
    unnest(sqlc.arg(dates)::date[]),
    unnest(sqlc.arg(amounts)::numeric[]),
    unnest(sqlc.arg(directions)::text[])
) WITH ORDINALITY AS candidates(date, amount, direction, ordinal)
JOIN financial_horizon_transactions t
  ON t.user_id = sqlc.arg(user_id)
 AND t.source = 'manual'
 AND (t.account_id IS NULL OR t.account_id = sqlc.arg(account_id)::bigint)
 AND t.amount = candidates.amount
 AND t.type = candidates.direction
 AND t.transaction_date >= (candidates.date::timestamp AT TIME ZONE sqlc.arg(timezone)::text)
 AND t.transaction_date < ((candidates.date + 1)::timestamp AT TIME ZONE sqlc.arg(timezone)::text);
