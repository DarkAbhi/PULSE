BEGIN;

-- Each event references one source record. Details are read from that source so
-- edits and sync upserts can refresh the card without recording another event.
CREATE TABLE recent_activity (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT statement_timestamp(),
    kind TEXT NOT NULL,
    fuel_fillup_id bigint,
    air_fill_id bigint,
    maintenance_id bigint,
    vehicle_id bigint,
    schedule_vehicle_id bigint,
    schedule_kind text,
    attachment_id bigint,
    meal_id bigint,
    meal_time_id bigint,
    gym_visit_id bigint,
    workout_id bigint,
    exercise_id bigint,
    sleep_date date,
    rings_date date,
    transaction_id bigint,
    statement_id uuid,
    subscription_id bigint,
    budget_id bigint,
    obligation_id bigint,
    purchase_id bigint,
    account_id bigint,
    category_id bigint,
    payment_subscription_id BIGINT REFERENCES financial_horizon_subscriptions(id) ON DELETE CASCADE,
    FOREIGN KEY (fuel_fillup_id) REFERENCES vehicle_fuel_fillups (id) ON DELETE CASCADE,
    FOREIGN KEY (air_fill_id) REFERENCES vehicle_air_fills (id) ON DELETE CASCADE,
    FOREIGN KEY (maintenance_id) REFERENCES vehicle_maintenance_records (id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles (id) ON DELETE CASCADE,
    FOREIGN KEY (schedule_vehicle_id, schedule_kind) REFERENCES vehicle_maintenance_schedules (vehicle_id, kind) ON DELETE CASCADE,
    FOREIGN KEY (attachment_id) REFERENCES vehicle_maintenance_attachments (id) ON DELETE CASCADE,
    FOREIGN KEY (meal_id) REFERENCES meal_plans (id) ON DELETE CASCADE,
    FOREIGN KEY (meal_time_id) REFERENCES meal_times (id) ON DELETE CASCADE,
    FOREIGN KEY (gym_visit_id) REFERENCES gym_visits (id) ON DELETE CASCADE,
    FOREIGN KEY (workout_id) REFERENCES fitness_workouts (id) ON DELETE CASCADE,
    FOREIGN KEY (exercise_id) REFERENCES gym_visit_exercises (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id, sleep_date) REFERENCES fitness_sleep (user_id, sleep_date) ON DELETE CASCADE,
    FOREIGN KEY (user_id, rings_date) REFERENCES fitness_activity_rings (user_id, summary_date) ON DELETE CASCADE,
    FOREIGN KEY (transaction_id) REFERENCES financial_horizon_transactions (id) ON DELETE CASCADE,
    FOREIGN KEY (statement_id) REFERENCES statement_import_batches (id) ON DELETE CASCADE,
    FOREIGN KEY (subscription_id) REFERENCES financial_horizon_subscriptions (id) ON DELETE CASCADE,
    FOREIGN KEY (budget_id) REFERENCES financial_horizon_budgets (id) ON DELETE CASCADE,
    FOREIGN KEY (obligation_id) REFERENCES financial_horizon_deductions (id) ON DELETE CASCADE,
    FOREIGN KEY (purchase_id) REFERENCES next_month_purchases (id) ON DELETE CASCADE,
    FOREIGN KEY (account_id) REFERENCES financial_accounts (id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES financial_horizon_categories (id) ON DELETE CASCADE,
    CHECK ((kind = 'subscription_payment_linked') = (payment_subscription_id IS NOT NULL)),
    CHECK ((schedule_vehicle_id IS NULL) = (schedule_kind IS NULL)),
    CHECK (num_nonnulls(fuel_fillup_id, air_fill_id, maintenance_id, vehicle_id, schedule_vehicle_id, attachment_id, meal_id, meal_time_id, gym_visit_id, workout_id, exercise_id,
                        sleep_date, rings_date, transaction_id, statement_id, subscription_id, budget_id, obligation_id, purchase_id, account_id, category_id) = 1),
    CHECK (
        (kind = 'fuel_fillup' AND fuel_fillup_id IS NOT NULL)
        OR (kind = 'air_fill' AND air_fill_id IS NOT NULL)
        OR (kind = 'maintenance' AND maintenance_id IS NOT NULL)
        OR (kind = 'vehicle_added' AND vehicle_id IS NOT NULL)
        OR (kind = 'reminder_configured' AND schedule_vehicle_id IS NOT NULL AND schedule_kind IS NOT NULL)
        OR (kind = 'maintenance_attachment' AND attachment_id IS NOT NULL)
        OR (kind IN ('meal_added', 'meal_eaten') AND meal_id IS NOT NULL)
        OR (kind = 'meal_time_added' AND meal_time_id IS NOT NULL)
        OR (kind = 'workout_logged' AND gym_visit_id IS NOT NULL)
        OR (kind = 'workout_synced' AND workout_id IS NOT NULL)
        OR (kind = 'exercise_added' AND exercise_id IS NOT NULL)
        OR (kind = 'sleep_synced' AND sleep_date IS NOT NULL)
        OR (kind = 'daily_activity_synced' AND rings_date IS NOT NULL)
        OR (kind IN ('transaction_logged', 'subscription_payment_linked') AND transaction_id IS NOT NULL)
        OR (kind = 'statement_imported' AND statement_id IS NOT NULL)
        OR (kind = 'subscription_added' AND subscription_id IS NOT NULL)
        OR (kind = 'budget_added' AND budget_id IS NOT NULL)
        OR (kind = 'obligation_added' AND obligation_id IS NOT NULL)
        OR (kind = 'planned_purchase_added' AND purchase_id IS NOT NULL)
        OR (kind IN ('account_added', 'account_archived', 'account_restored') AND account_id IS NOT NULL)
        OR (kind = 'category_added' AND category_id IS NOT NULL)
    )
);

CREATE INDEX recent_activity_user_latest_idx ON recent_activity (user_id, recorded_at DESC, id DESC);

CREATE INDEX recent_activity_fuel_fillup_id_idx ON recent_activity (fuel_fillup_id) WHERE fuel_fillup_id IS NOT NULL;
CREATE INDEX recent_activity_air_fill_id_idx ON recent_activity (air_fill_id) WHERE air_fill_id IS NOT NULL;
CREATE INDEX recent_activity_maintenance_id_idx ON recent_activity (maintenance_id) WHERE maintenance_id IS NOT NULL;
CREATE INDEX recent_activity_vehicle_id_idx ON recent_activity (vehicle_id) WHERE vehicle_id IS NOT NULL;
CREATE INDEX recent_activity_schedule_vehicle_id_idx ON recent_activity (schedule_vehicle_id, schedule_kind) WHERE schedule_vehicle_id IS NOT NULL;
CREATE INDEX recent_activity_attachment_id_idx ON recent_activity (attachment_id) WHERE attachment_id IS NOT NULL;
CREATE INDEX recent_activity_meal_id_idx ON recent_activity (meal_id) WHERE meal_id IS NOT NULL;
CREATE INDEX recent_activity_meal_time_id_idx ON recent_activity (meal_time_id) WHERE meal_time_id IS NOT NULL;
CREATE INDEX recent_activity_gym_visit_id_idx ON recent_activity (gym_visit_id) WHERE gym_visit_id IS NOT NULL;
CREATE UNIQUE INDEX recent_activity_workout_id_idx ON recent_activity (workout_id) WHERE workout_id IS NOT NULL;
CREATE INDEX recent_activity_exercise_id_idx ON recent_activity (exercise_id) WHERE exercise_id IS NOT NULL;
CREATE UNIQUE INDEX recent_activity_sleep_date_idx ON recent_activity (user_id, sleep_date) WHERE sleep_date IS NOT NULL;
CREATE UNIQUE INDEX recent_activity_rings_date_idx ON recent_activity (user_id, rings_date) WHERE rings_date IS NOT NULL;
CREATE INDEX recent_activity_transaction_id_idx ON recent_activity (transaction_id) WHERE transaction_id IS NOT NULL;
CREATE UNIQUE INDEX recent_activity_statement_id_idx ON recent_activity (statement_id) WHERE statement_id IS NOT NULL;
CREATE INDEX recent_activity_subscription_id_idx ON recent_activity (subscription_id) WHERE subscription_id IS NOT NULL;
CREATE INDEX recent_activity_budget_id_idx ON recent_activity (budget_id) WHERE budget_id IS NOT NULL;
CREATE INDEX recent_activity_obligation_id_idx ON recent_activity (obligation_id) WHERE obligation_id IS NOT NULL;
CREATE INDEX recent_activity_purchase_id_idx ON recent_activity (purchase_id) WHERE purchase_id IS NOT NULL;
CREATE INDEX recent_activity_account_id_idx ON recent_activity (account_id) WHERE account_id IS NOT NULL;
CREATE INDEX recent_activity_category_id_idx ON recent_activity (category_id) WHERE category_id IS NOT NULL;
CREATE INDEX recent_activity_payment_subscription_idx ON recent_activity (payment_subscription_id) WHERE payment_subscription_id IS NOT NULL;

-- AFTER INSERT fires for new rows only. ON CONFLICT DO UPDATE on either Rust
-- sync route leaves the existing activity's recorded_at untouched.
CREATE FUNCTION capture_recent_activity() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    event_kind TEXT := TG_ARGV[0];
BEGIN
    CASE event_kind
        WHEN 'fuel_fillup' THEN
            INSERT INTO recent_activity (kind, user_id, fuel_fillup_id) VALUES (event_kind, NEW.user_id, NEW.id);
        WHEN 'air_fill' THEN
            INSERT INTO recent_activity (kind, user_id, air_fill_id) VALUES (event_kind, NEW.user_id, NEW.id);
        WHEN 'maintenance' THEN
            INSERT INTO recent_activity (kind, user_id, maintenance_id) VALUES (event_kind, NEW.user_id, NEW.id);
        WHEN 'vehicle_added' THEN
            INSERT INTO recent_activity (kind, user_id, vehicle_id) VALUES (event_kind, NEW.user_id, NEW.id);
        WHEN 'reminder_configured' THEN
            INSERT INTO recent_activity (kind, user_id, schedule_vehicle_id, schedule_kind) VALUES (event_kind, NEW.user_id, NEW.vehicle_id, NEW.kind);
        WHEN 'maintenance_attachment' THEN
            INSERT INTO recent_activity (kind, user_id, attachment_id) VALUES (event_kind, NEW.user_id, NEW.id);
        WHEN 'meal_added', 'meal_eaten' THEN
            INSERT INTO recent_activity (kind, user_id, meal_id) VALUES (event_kind, NEW.user_id, NEW.id);
        WHEN 'meal_time_added' THEN
            INSERT INTO recent_activity (kind, user_id, meal_time_id) VALUES (event_kind, NEW.user_id, NEW.id);
        WHEN 'workout_logged' THEN
            INSERT INTO recent_activity (kind, user_id, gym_visit_id) VALUES (event_kind, NEW.user_id, NEW.id);
        WHEN 'workout_synced' THEN
            INSERT INTO recent_activity (kind, user_id, workout_id) VALUES (event_kind, NEW.user_id, NEW.id);
        WHEN 'sleep_synced' THEN
            INSERT INTO recent_activity (kind, user_id, sleep_date) VALUES (event_kind, NEW.user_id, NEW.sleep_date);
        WHEN 'daily_activity_synced' THEN
            INSERT INTO recent_activity (kind, user_id, rings_date) VALUES (event_kind, NEW.user_id, NEW.summary_date);
        WHEN 'transaction_logged' THEN
            INSERT INTO recent_activity (kind, user_id, transaction_id) VALUES (event_kind, NEW.user_id, NEW.id);
        WHEN 'subscription_added' THEN
            INSERT INTO recent_activity (kind, user_id, subscription_id) VALUES (event_kind, NEW.user_id, NEW.id);
        WHEN 'budget_added' THEN
            INSERT INTO recent_activity (kind, user_id, budget_id) VALUES (event_kind, NEW.user_id, NEW.id);
        WHEN 'obligation_added' THEN
            INSERT INTO recent_activity (kind, user_id, obligation_id) VALUES (event_kind, NEW.user_id, NEW.id);
        WHEN 'planned_purchase_added' THEN
            INSERT INTO recent_activity (kind, user_id, purchase_id) VALUES (event_kind, NEW.user_id, NEW.id);
        WHEN 'account_added', 'account_archived', 'account_restored' THEN
            INSERT INTO recent_activity (kind, user_id, account_id) VALUES (event_kind, NEW.user_id, NEW.id);
        WHEN 'category_added' THEN
            INSERT INTO recent_activity (kind, user_id, category_id) VALUES (event_kind, NEW.user_id, NEW.id);
        WHEN 'exercise_added' THEN
            INSERT INTO recent_activity (kind, user_id, exercise_id)
            SELECT event_kind, user_id, NEW.id FROM gym_visits WHERE id = NEW.gym_visit_id;
        WHEN 'statement_imported' THEN
            INSERT INTO recent_activity (kind, user_id, statement_id)
            VALUES (event_kind, NEW.user_id, NEW.id);
    END CASE;
    RETURN NEW;
END;
$$;

CREATE TRIGGER recent_activity_fuel_fillup AFTER INSERT ON vehicle_fuel_fillups
FOR EACH ROW EXECUTE FUNCTION capture_recent_activity('fuel_fillup');

CREATE TRIGGER recent_activity_air_fill AFTER INSERT ON vehicle_air_fills
FOR EACH ROW EXECUTE FUNCTION capture_recent_activity('air_fill');

CREATE TRIGGER recent_activity_maintenance AFTER INSERT ON vehicle_maintenance_records
FOR EACH ROW EXECUTE FUNCTION capture_recent_activity('maintenance');

CREATE TRIGGER recent_activity_vehicle_added AFTER INSERT ON vehicles
FOR EACH ROW EXECUTE FUNCTION capture_recent_activity('vehicle_added');

CREATE TRIGGER recent_activity_reminder_configured AFTER INSERT ON vehicle_maintenance_schedules
FOR EACH ROW EXECUTE FUNCTION capture_recent_activity('reminder_configured');

CREATE TRIGGER recent_activity_maintenance_attachment AFTER INSERT ON vehicle_maintenance_attachments
FOR EACH ROW EXECUTE FUNCTION capture_recent_activity('maintenance_attachment');

CREATE TRIGGER recent_activity_meal_added AFTER INSERT ON meal_plans
FOR EACH ROW EXECUTE FUNCTION capture_recent_activity('meal_added');

CREATE TRIGGER recent_activity_meal_eaten AFTER UPDATE OF is_consumed ON meal_plans
FOR EACH ROW WHEN (NEW.is_consumed AND NOT OLD.is_consumed) EXECUTE FUNCTION capture_recent_activity('meal_eaten');

CREATE TRIGGER recent_activity_meal_time_added AFTER INSERT ON meal_times
FOR EACH ROW WHEN (NEW.user_id IS NOT NULL) EXECUTE FUNCTION capture_recent_activity('meal_time_added');

CREATE TRIGGER recent_activity_workout_logged AFTER INSERT ON gym_visits
FOR EACH ROW WHEN (NEW.fitness_workout_id IS NULL) EXECUTE FUNCTION capture_recent_activity('workout_logged');

CREATE TRIGGER recent_activity_workout_synced AFTER INSERT ON fitness_workouts
FOR EACH ROW EXECUTE FUNCTION capture_recent_activity('workout_synced');

CREATE TRIGGER recent_activity_exercise_added AFTER INSERT ON gym_visit_exercises
FOR EACH ROW EXECUTE FUNCTION capture_recent_activity('exercise_added');

CREATE TRIGGER recent_activity_sleep_synced AFTER INSERT ON fitness_sleep
FOR EACH ROW EXECUTE FUNCTION capture_recent_activity('sleep_synced');

CREATE TRIGGER recent_activity_daily_activity_synced AFTER INSERT ON fitness_activity_rings
FOR EACH ROW EXECUTE FUNCTION capture_recent_activity('daily_activity_synced');

CREATE TRIGGER recent_activity_transaction_logged AFTER INSERT ON financial_horizon_transactions
FOR EACH ROW WHEN (NEW.source = 'manual') EXECUTE FUNCTION capture_recent_activity('transaction_logged');

CREATE TRIGGER recent_activity_subscription_added AFTER INSERT ON financial_horizon_subscriptions
FOR EACH ROW EXECUTE FUNCTION capture_recent_activity('subscription_added');

CREATE TRIGGER recent_activity_budget_added AFTER INSERT ON financial_horizon_budgets
FOR EACH ROW EXECUTE FUNCTION capture_recent_activity('budget_added');

CREATE TRIGGER recent_activity_obligation_added AFTER INSERT ON financial_horizon_deductions
FOR EACH ROW EXECUTE FUNCTION capture_recent_activity('obligation_added');

CREATE TRIGGER recent_activity_planned_purchase_added AFTER INSERT ON next_month_purchases
FOR EACH ROW EXECUTE FUNCTION capture_recent_activity('planned_purchase_added');

CREATE TRIGGER recent_activity_account_added AFTER INSERT ON financial_accounts
FOR EACH ROW EXECUTE FUNCTION capture_recent_activity('account_added');

CREATE TRIGGER recent_activity_account_archived AFTER UPDATE OF archived_at ON financial_accounts
FOR EACH ROW WHEN (OLD.archived_at IS NULL AND NEW.archived_at IS NOT NULL) EXECUTE FUNCTION capture_recent_activity('account_archived');

CREATE TRIGGER recent_activity_account_restored AFTER UPDATE OF archived_at ON financial_accounts
FOR EACH ROW WHEN (OLD.archived_at IS NOT NULL AND NEW.archived_at IS NULL) EXECUTE FUNCTION capture_recent_activity('account_restored');

CREATE TRIGGER recent_activity_category_added AFTER INSERT ON financial_horizon_categories
FOR EACH ROW WHEN (NEW.user_id IS NOT NULL) EXECUTE FUNCTION capture_recent_activity('category_added');

CREATE TRIGGER recent_activity_statement_imported AFTER UPDATE OF status ON statement_import_batches
FOR EACH ROW WHEN (NEW.status = 'committed' AND OLD.status <> 'committed' AND NEW.imported_count > 0)
EXECUTE FUNCTION capture_recent_activity('statement_imported');

COMMIT;
