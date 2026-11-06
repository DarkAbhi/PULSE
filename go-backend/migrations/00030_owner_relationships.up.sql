BEGIN;

-- Keep existing single-column foreign keys for their CASCADE / SET NULL behavior.
-- These additional keys prevent references across owners, including direct SQL writes.

ALTER TABLE vehicles ADD CONSTRAINT vehicles_owner_id_key UNIQUE (user_id, id);

ALTER TABLE vehicle_maintenance_records ADD CONSTRAINT vehicle_maintenance_records_owner_id_key UNIQUE (user_id, id);

ALTER TABLE notifications ADD CONSTRAINT notifications_owner_id_key UNIQUE (user_id, id);

ALTER TABLE fitness_workouts ADD CONSTRAINT fitness_workouts_owner_id_key UNIQUE (user_id, id);

ALTER TABLE financial_horizon_budgets ADD CONSTRAINT financial_horizon_budgets_owner_id_key UNIQUE (user_id, id);

ALTER TABLE financial_horizon_deductions ADD CONSTRAINT financial_horizon_deductions_owner_id_key UNIQUE (user_id, id);

ALTER TABLE financial_horizon_subscriptions ADD CONSTRAINT financial_horizon_subscriptions_owner_id_key UNIQUE (user_id, id);

ALTER TABLE vehicle_air_fills ADD CONSTRAINT vehicle_air_fills_vehicle_id_owner_fk
    FOREIGN KEY (user_id, vehicle_id) REFERENCES vehicles (user_id, id);

ALTER TABLE vehicle_fuel_fillups ADD CONSTRAINT vehicle_fuel_fillups_vehicle_id_owner_fk
    FOREIGN KEY (user_id, vehicle_id) REFERENCES vehicles (user_id, id);

ALTER TABLE vehicle_maintenance_records ADD CONSTRAINT vehicle_maintenance_records_vehicle_id_owner_fk
    FOREIGN KEY (user_id, vehicle_id) REFERENCES vehicles (user_id, id);

ALTER TABLE vehicle_maintenance_attachments ADD CONSTRAINT vehicle_maintenance_attachments_maintenance_record_id_owner_fk
    FOREIGN KEY (user_id, maintenance_record_id) REFERENCES vehicle_maintenance_records (user_id, id);

ALTER TABLE vehicle_air_fills ADD CONSTRAINT vehicle_air_fills_reminder_notification_id_owner_fk
    FOREIGN KEY (user_id, reminder_notification_id) REFERENCES notifications (user_id, id);

ALTER TABLE gym_visits ADD CONSTRAINT gym_visits_fitness_workout_id_owner_fk
    FOREIGN KEY (user_id, fitness_workout_id) REFERENCES fitness_workouts (user_id, id);

ALTER TABLE financial_horizon_deductions ADD CONSTRAINT financial_horizon_deductions_budget_id_owner_fk
    FOREIGN KEY (user_id, budget_id) REFERENCES financial_horizon_budgets (user_id, id);

ALTER TABLE financial_horizon_transactions ADD CONSTRAINT financial_horizon_transactions_budget_id_owner_fk
    FOREIGN KEY (user_id, budget_id) REFERENCES financial_horizon_budgets (user_id, id);

ALTER TABLE financial_horizon_transactions ADD CONSTRAINT financial_horizon_transactions_subscription_id_owner_fk
    FOREIGN KEY (user_id, subscription_id) REFERENCES financial_horizon_subscriptions (user_id, id);

ALTER TABLE financial_horizon_subscriptions ADD CONSTRAINT financial_horizon_subscriptions_budget_id_owner_fk
    FOREIGN KEY (user_id, budget_id) REFERENCES financial_horizon_budgets (user_id, id);

ALTER TABLE financial_horizon_subscriptions ADD CONSTRAINT financial_horizon_subscriptions_deduction_id_owner_fk
    FOREIGN KEY (user_id, deduction_id) REFERENCES financial_horizon_deductions (user_id, id);

ALTER TABLE financial_horizon_categories ADD CONSTRAINT financial_horizon_categories_default_owner_check
    CHECK ((user_id IS NULL) = is_default);

ALTER TABLE meal_times ADD CONSTRAINT meal_times_default_owner_check
    CHECK ((user_id IS NULL) = is_default);

COMMIT;
