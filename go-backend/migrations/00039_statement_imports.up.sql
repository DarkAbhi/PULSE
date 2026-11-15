CREATE TABLE statement_import_batches (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id BIGINT NOT NULL,
    source_sha256 TEXT NOT NULL CHECK (length(source_sha256) = 64),
    status TEXT NOT NULL CHECK (status IN ('ready', 'failed', 'unsupported', 'committed', 'cancelled', 'expired')),
    profile_id TEXT NOT NULL,
    currency TEXT NOT NULL,
    ocr_used BOOLEAN NOT NULL DEFAULT false,
    reason_code TEXT NOT NULL DEFAULT '',
    revision INTEGER NOT NULL DEFAULT 1,
    account_confirmed BOOLEAN NOT NULL,
    parsed_count INTEGER NOT NULL DEFAULT 0,
    imported_count INTEGER NOT NULL DEFAULT 0,
    duplicate_count INTEGER NOT NULL DEFAULT 0,
    excluded_count INTEGER NOT NULL DEFAULT 0,
    opening_minor BIGINT,
    closing_minor BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    committed_at TIMESTAMPTZ,
    UNIQUE (id, user_id, account_id),
    FOREIGN KEY (account_id, user_id) REFERENCES financial_accounts(id, user_id) ON DELETE CASCADE
);
CREATE INDEX statement_import_batches_source_idx ON statement_import_batches(user_id, account_id, source_sha256);
CREATE INDEX statement_import_batches_expiry_idx ON statement_import_batches(expires_at) WHERE status IN ('ready', 'unsupported');

CREATE TABLE statement_import_rows (
    batch_id UUID NOT NULL REFERENCES statement_import_batches(id) ON DELETE CASCADE,
    ordinal INTEGER NOT NULL CHECK (ordinal > 0),
    source JSONB NOT NULL,
    preview JSONB NOT NULL,
    PRIMARY KEY (batch_id, ordinal)
);
CREATE TABLE statement_import_structures (
    batch_id UUID PRIMARY KEY REFERENCES statement_import_batches(id) ON DELETE CASCADE,
    document JSONB NOT NULL
);
CREATE TABLE statement_import_dedupe (
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    fingerprint TEXT NOT NULL CHECK (length(fingerprint) = 64),
    occurrence INTEGER NOT NULL CHECK (occurrence > 0),
    batch_id UUID NOT NULL,
    ordinal INTEGER NOT NULL,
    PRIMARY KEY (user_id, account_id, fingerprint, occurrence),
    FOREIGN KEY (batch_id, user_id, account_id) REFERENCES statement_import_batches(id, user_id, account_id) ON DELETE CASCADE
);
CREATE TABLE statement_category_rules (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    merchant_key TEXT NOT NULL,
    category_id BIGINT NOT NULL REFERENCES financial_horizon_categories(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, merchant_key)
);

ALTER TABLE financial_horizon_transactions
    ADD COLUMN source TEXT NOT NULL DEFAULT 'manual' CHECK (source IN ('manual', 'statement')),
    ADD COLUMN import_batch_id UUID,
    ADD COLUMN import_row_ordinal INTEGER,
    ADD COLUMN dedupe_hash TEXT,
    ADD CONSTRAINT statement_transaction_batch_owner_fk
        FOREIGN KEY (import_batch_id, user_id, account_id) REFERENCES statement_import_batches(id, user_id, account_id),
    ADD CONSTRAINT statement_transaction_source CHECK (
        (source = 'manual' AND import_batch_id IS NULL AND import_row_ordinal IS NULL AND dedupe_hash IS NULL)
        OR (source = 'statement' AND account_id IS NOT NULL AND import_batch_id IS NOT NULL AND import_row_ordinal IS NOT NULL AND import_row_ordinal > 0 AND dedupe_hash IS NOT NULL)
    ),
    ADD UNIQUE (import_batch_id, import_row_ordinal);
