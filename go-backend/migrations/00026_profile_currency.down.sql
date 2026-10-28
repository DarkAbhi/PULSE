-- Keep the preference usable by older releases before removing the profile field.
UPDATE financial_horizon_configs c SET currency = p.currency
FROM user_profiles p WHERE p.user_id = c.user_id;
ALTER TABLE user_profiles DROP COLUMN currency;
