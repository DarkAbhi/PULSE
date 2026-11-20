ALTER TABLE financial_horizon_subscriptions
DROP CONSTRAINT IF EXISTS financial_horizon_subscriptions_billing_cycle_check;

ALTER TABLE financial_horizon_subscriptions
ADD CONSTRAINT financial_horizon_subscriptions_billing_cycle_check
CHECK (billing_cycle IN ('weekly', 'monthly', 'quarterly', 'yearly'));

ALTER TABLE financial_horizon_subscriptions
DROP COLUMN IF EXISTS renewal_date;
