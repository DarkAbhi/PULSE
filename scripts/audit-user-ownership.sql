-- Read-only audit for installations at schema version 28 or later.
-- Run with psql -X -v ON_ERROR_STOP=1 --dbname <database> -f scripts/audit-user-ownership.sql
BEGIN TRANSACTION READ ONLY;
SELECT version, dirty FROM schema_migrations;
SELECT count(*) AS user_count FROM users;
SELECT table_name, column_name, is_nullable FROM information_schema.columns
WHERE table_schema='public' AND column_name='user_id' ORDER BY table_name;

-- Audit legacy ownership before migration 29; migration 31 removes these tables.
SELECT format('SELECT %L AS table_name, count(*) AS unowned_rows FROM public.%I r WHERE to_jsonb(r)->>''user_id'' IS NULL', tablename, tablename)
FROM pg_tables WHERE schemaname='public' AND tablename IN ('meditations', 'credit_cards', 'sports')
\gexec

SELECT 'vehicle_air_fills.vehicle_id' AS relationship, count(*) AS mismatches
FROM vehicle_air_fills child JOIN vehicles parent ON parent.id=child.vehicle_id
WHERE child.user_id <> parent.user_id
UNION ALL
SELECT 'vehicle_fuel_fillups.vehicle_id' AS relationship, count(*) AS mismatches
FROM vehicle_fuel_fillups child JOIN vehicles parent ON parent.id=child.vehicle_id
WHERE child.user_id <> parent.user_id
UNION ALL
SELECT 'vehicle_maintenance_records.vehicle_id' AS relationship, count(*) AS mismatches
FROM vehicle_maintenance_records child JOIN vehicles parent ON parent.id=child.vehicle_id
WHERE child.user_id <> parent.user_id
UNION ALL
SELECT 'vehicle_maintenance_attachments.maintenance_record_id' AS relationship, count(*) AS mismatches
FROM vehicle_maintenance_attachments child JOIN vehicle_maintenance_records parent ON parent.id=child.maintenance_record_id
WHERE child.user_id <> parent.user_id
UNION ALL
SELECT 'vehicle_air_fills.reminder_notification_id' AS relationship, count(*) AS mismatches
FROM vehicle_air_fills child JOIN notifications parent ON parent.id=child.reminder_notification_id
WHERE child.user_id <> parent.user_id
UNION ALL
SELECT 'gym_visits.fitness_workout_id' AS relationship, count(*) AS mismatches
FROM gym_visits child JOIN fitness_workouts parent ON parent.id=child.fitness_workout_id
WHERE child.user_id <> parent.user_id
UNION ALL
SELECT 'financial_horizon_deductions.budget_id' AS relationship, count(*) AS mismatches
FROM financial_horizon_deductions child JOIN financial_horizon_budgets parent ON parent.id=child.budget_id
WHERE child.user_id <> parent.user_id
UNION ALL
SELECT 'financial_horizon_transactions.budget_id' AS relationship, count(*) AS mismatches
FROM financial_horizon_transactions child JOIN financial_horizon_budgets parent ON parent.id=child.budget_id
WHERE child.user_id <> parent.user_id
UNION ALL
SELECT 'financial_horizon_transactions.subscription_id' AS relationship, count(*) AS mismatches
FROM financial_horizon_transactions child JOIN financial_horizon_subscriptions parent ON parent.id=child.subscription_id
WHERE child.user_id <> parent.user_id
UNION ALL
SELECT 'financial_horizon_subscriptions.budget_id' AS relationship, count(*) AS mismatches
FROM financial_horizon_subscriptions child JOIN financial_horizon_budgets parent ON parent.id=child.budget_id
WHERE child.user_id <> parent.user_id
UNION ALL
SELECT 'financial_horizon_subscriptions.deduction_id' AS relationship, count(*) AS mismatches
FROM financial_horizon_subscriptions child JOIN financial_horizon_deductions parent ON parent.id=child.deduction_id
WHERE child.user_id <> parent.user_id
UNION ALL
SELECT 'financial_horizon_transactions.category_id', count(*)
FROM financial_horizon_transactions child JOIN financial_horizon_categories parent ON parent.id=child.category_id
WHERE NOT (parent.user_id=child.user_id OR (parent.user_id IS NULL AND parent.is_default))
UNION ALL
SELECT 'financial_horizon_subscriptions.category_id', count(*)
FROM financial_horizon_subscriptions child JOIN financial_horizon_categories parent ON parent.id=child.category_id
WHERE NOT (parent.user_id=child.user_id OR (parent.user_id IS NULL AND parent.is_default))
UNION ALL
SELECT 'meal_plans.meal_time_id', count(*)
FROM meal_plans child JOIN meal_times parent ON parent.id=child.meal_time_id
WHERE NOT (parent.user_id=child.user_id OR (parent.user_id IS NULL AND parent.is_default));

SELECT 'financial_horizon_categories' AS table_name, count(*) AS invalid_shared_rows FROM financial_horizon_categories WHERE (user_id IS NULL) <> is_default
UNION ALL
SELECT 'meal_times' AS table_name, count(*) AS invalid_shared_rows FROM meal_times WHERE (user_id IS NULL) <> is_default;
COMMIT;
