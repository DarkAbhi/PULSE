BEGIN;
-- Account data is development-only and intentionally discarded.
-- Keep transaction amounts/history while removing account/import references.
LOCK TABLE financial_accounts, financial_horizon_transactions IN ACCESS EXCLUSIVE MODE;
UPDATE financial_horizon_transactions SET account_id = NULL, source = 'manual',
    import_batch_id = NULL, import_row_ordinal = NULL, dedupe_hash = NULL
WHERE account_id IS NOT NULL;
DELETE FROM financial_accounts;

ALTER TABLE financial_accounts
    ADD COLUMN institution_name VARCHAR(150) NOT NULL DEFAULT '',
    ADD COLUMN bank_account_subtype TEXT NOT NULL DEFAULT '',
    ADD COLUMN currency TEXT NOT NULL DEFAULT 'GBP',
    ADD COLUMN country_code TEXT NOT NULL DEFAULT '',
    ADD COLUMN last_four TEXT NOT NULL DEFAULT '';
DROP VIEW financial_account_details;
DROP TABLE bank_accounts, credit_cards;
ALTER TABLE financial_accounts ADD CHECK (length(trim(institution_name)) > 0),
    ADD CHECK (bank_account_subtype IN ('', 'savings', 'current', 'other')),
    ADD CHECK (account_type = 'bank_account' OR bank_account_subtype = ''),
    ADD CHECK (currency ~ '^[A-Z]{3}$'),
    ADD CHECK (country_code = '' OR country_code ~ '^[A-Z]{2}$'),
    ADD CHECK (last_four = '' OR last_four ~ '^[0-9]{4}$');
COMMIT;
