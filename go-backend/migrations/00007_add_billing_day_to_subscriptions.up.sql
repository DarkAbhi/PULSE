ALTER TABLE financial_horizon_subscriptions
ADD COLUMN IF NOT EXISTS billing_day INT NOT NULL DEFAULT 1 CHECK (billing_day >= 1 AND billing_day <= 31);
