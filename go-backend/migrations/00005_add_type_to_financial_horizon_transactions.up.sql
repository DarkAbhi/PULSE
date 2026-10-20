ALTER TABLE financial_horizon_transactions
ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'debit' CHECK (type IN ('debit', 'credit'));
