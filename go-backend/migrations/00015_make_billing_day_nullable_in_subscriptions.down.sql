UPDATE financial_horizon_subscriptions
    SET billing_day = 1
    WHERE billing_day IS NULL;

ALTER TABLE financial_horizon_subscriptions
    ALTER COLUMN billing_day SET NOT NULL;
