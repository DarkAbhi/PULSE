BEGIN;

-- Account data is development-only and intentionally discarded.
-- Keep transaction amounts/history while removing account/import references.
LOCK TABLE financial_accounts, financial_horizon_transactions IN ACCESS EXCLUSIVE MODE;
UPDATE financial_horizon_transactions SET account_id = NULL, source = 'manual',
    import_batch_id = NULL, import_row_ordinal = NULL, dedupe_hash = NULL
WHERE account_id IS NOT NULL;
DELETE FROM financial_accounts;

-- Shared IDs retain ownership-enforcing foreign keys for new accounts.
CREATE TABLE bank_accounts (
    account_id BIGINT PRIMARY KEY REFERENCES financial_accounts(id) ON DELETE CASCADE,
    institution_name VARCHAR(150) NOT NULL,
    account_number VARCHAR(34) NOT NULL DEFAULT '' CHECK (account_number = '' OR account_number ~ '^[A-Za-z0-9]{4,34}$'),
    holder_name VARCHAR(150) NOT NULL CHECK (length(trim(holder_name)) > 0),
    holder_email VARCHAR(254) NOT NULL CHECK (length(trim(holder_email)) > 0),
    bank_account_subtype TEXT NOT NULL CHECK (bank_account_subtype IN ('savings', 'current')),
    last_four TEXT NOT NULL DEFAULT '' CHECK (last_four = '' OR last_four ~ '^[0-9]{4}$')
);
CREATE TABLE credit_cards (
    account_id BIGINT PRIMARY KEY REFERENCES financial_accounts(id) ON DELETE CASCADE,
    institution_name VARCHAR(150) NOT NULL,
    card_type VARCHAR(150) NOT NULL CHECK (length(trim(card_type)) > 0),
    card_network TEXT NOT NULL CHECK (card_network IN ( 'visa', 'mastercard', 'american_express', 'diners_club', 'rupay')),
    last_four TEXT NOT NULL DEFAULT '' CHECK (last_four = '' OR last_four ~ '^[0-9]{4}$'),
    holder_name VARCHAR(150) NOT NULL CHECK (length(trim(holder_name)) > 0),
    credit_limit NUMERIC NOT NULL CHECK (credit_limit >= 0 AND credit_limit < 1000000000000000),
    statement_day INTEGER NOT NULL CHECK (statement_day BETWEEN 1 AND 31)
);

ALTER TABLE financial_accounts DROP COLUMN institution_name, DROP COLUMN bank_account_subtype,
    DROP COLUMN currency, DROP COLUMN country_code, DROP COLUMN last_four;

CREATE VIEW financial_account_details AS
SELECT a.*, COALESCE(b.institution_name, c.institution_name, '')::varchar(150) AS institution_name,
    COALESCE(b.bank_account_subtype, '')::text AS bank_account_subtype,
    COALESCE(b.last_four, c.last_four, '')::text AS last_four,
    COALESCE(b.account_number, '')::varchar(34) AS account_number,
    COALESCE(b.holder_name, c.holder_name, '')::varchar(150) AS holder_name,
    COALESCE(b.holder_email, '')::varchar(254) AS holder_email,
    COALESCE(c.card_type, '')::varchar(150) AS card_type,
    COALESCE(c.card_network, '')::text AS card_network,
    c.credit_limit, c.statement_day
FROM financial_accounts a
LEFT JOIN bank_accounts b ON b.account_id = a.id AND a.account_type = 'bank_account'
LEFT JOIN credit_cards c ON c.account_id = a.id AND a.account_type = 'credit_card';

COMMIT;
