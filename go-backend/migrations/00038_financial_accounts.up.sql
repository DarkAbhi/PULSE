CREATE TABLE financial_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(100) NOT NULL CHECK (length(trim(display_name)) > 0),
    institution_name VARCHAR(150) NOT NULL CHECK (length(trim(institution_name)) > 0),
    account_type TEXT NOT NULL CHECK (account_type IN ('bank_account', 'credit_card')),
    bank_account_subtype TEXT NOT NULL DEFAULT '' CHECK (bank_account_subtype IN ('', 'savings', 'current', 'other')),
    currency TEXT NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    country_code TEXT NOT NULL DEFAULT '' CHECK (country_code = '' OR country_code ~ '^[A-Z]{2}$'),
    last_four TEXT NOT NULL DEFAULT '' CHECK (last_four = '' OR last_four ~ '^[0-9]{4}$'),
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id, user_id),
    CHECK (account_type = 'bank_account' OR bank_account_subtype = '')
);
CREATE INDEX financial_accounts_user_idx ON financial_accounts (user_id, id);

ALTER TABLE financial_horizon_transactions ADD COLUMN account_id BIGINT;
ALTER TABLE financial_horizon_transactions ADD CONSTRAINT horizon_transaction_account_owner_fk
    FOREIGN KEY (account_id, user_id) REFERENCES financial_accounts (id, user_id);
CREATE INDEX horizon_transactions_account_idx ON financial_horizon_transactions (user_id, account_id);
