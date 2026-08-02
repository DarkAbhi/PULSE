ALTER TABLE financial_horizon_subscriptions
DROP CONSTRAINT IF EXISTS financial_horizon_subscriptions_billing_cycle_check;

ALTER TABLE financial_horizon_subscriptions
ADD CONSTRAINT financial_horizon_subscriptions_billing_cycle_check
CHECK (billing_cycle IN ('monthly', 'yearly'));

ALTER TABLE financial_horizon_subscriptions
ADD COLUMN IF NOT EXISTS renewal_date TIMESTAMPTZ NULL;
