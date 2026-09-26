-- Development baseline: all event timestamps are absolute UTC-aware instants.
-- Calendar-only values intentionally use DATE.

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX user_sessions_token_hash_idx ON user_sessions (token_hash);
CREATE INDEX user_sessions_expires_at_idx ON user_sessions (expires_at);

CREATE TABLE user_profiles (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE meditations (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE gym_visits (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE gym_visit_exercises (
    id BIGSERIAL PRIMARY KEY,
    gym_visit_id BIGINT NOT NULL REFERENCES gym_visits(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX gym_visit_exercises_visit_id_idx ON gym_visit_exercises (gym_visit_id);

CREATE TABLE gym_exercise_sets (
    id BIGSERIAL PRIMARY KEY,
    gym_visit_exercise_id BIGINT NOT NULL REFERENCES gym_visit_exercises(id) ON DELETE CASCADE,
    set_number SMALLINT NOT NULL CHECK (set_number > 0),
    reps SMALLINT NOT NULL CHECK (reps > 0),
    weight NUMERIC(8,2) CHECK (weight IS NULL OR weight >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (gym_visit_exercise_id, set_number)
);

CREATE TABLE credit_cards (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    link TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sports (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(10) NOT NULL CHECK (name IN ('cricket', 'football', 'badminton')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(48) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source VARCHAR(64) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body TEXT,
    target_path VARCHAR(512),
    priority SMALLINT NOT NULL DEFAULT 0 CHECK (priority BETWEEN 0 AND 2),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    read_at TIMESTAMPTZ,
    dismissed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX notifications_active_by_user_idx
    ON notifications (user_id, created_at DESC)
    WHERE dismissed_at IS NULL;

CREATE TABLE vehicle_air_fills (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    filled_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reminder_notification_id BIGINT UNIQUE REFERENCES notifications(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX vehicle_air_fills_latest_by_vehicle_idx
    ON vehicle_air_fills (vehicle_id, user_id, filled_at DESC);
CREATE INDEX vehicle_air_fills_due_reminders_idx
    ON vehicle_air_fills (filled_at)
    WHERE reminder_notification_id IS NULL;

CREATE TABLE vehicle_fuel_fillups (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    odometer_km NUMERIC(12,1) NOT NULL CHECK (odometer_km >= 0),
    filled_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    station_name VARCHAR(160),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX vehicle_fuel_fillups_vehicle_idx ON vehicle_fuel_fillups (vehicle_id, filled_at, id);

CREATE TABLE vehicle_fuel_items (
    id BIGSERIAL PRIMARY KEY,
    fillup_id BIGINT NOT NULL REFERENCES vehicle_fuel_fillups(id) ON DELETE CASCADE,
    fuel_type VARCHAR(16) NOT NULL CHECK (fuel_type IN ('petrol', 'diesel', 'lpg', 'cng', 'electric')),
    fill_type VARCHAR(16) NOT NULL CHECK (fill_type IN ('full', 'partial', 'missed')),
    quantity NUMERIC(10,3) NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(10,2) NOT NULL CHECK (unit_price > 0),
    total_cost NUMERIC(12,2) NOT NULL CHECK (total_cost > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX vehicle_fuel_items_fillup_idx ON vehicle_fuel_items (fillup_id, fuel_type);

CREATE TABLE next_month_purchases (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_month DATE NOT NULL,
    name VARCHAR(160) NOT NULL,
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX next_month_purchases_user_month_idx ON next_month_purchases (user_id, target_month, created_at);

CREATE TABLE gym_reminder_deliveries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reminder_date DATE NOT NULL,
    notification_id BIGINT NOT NULL UNIQUE REFERENCES notifications(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, reminder_date)
);

CREATE INDEX gym_reminder_deliveries_date_idx ON gym_reminder_deliveries (reminder_date);

-- Development-only bootstrap account: username "admin", password "password".
INSERT INTO users (username, password_hash)
VALUES ('admin', '$2y$12$bB7WwVq7nGJ4cfNTCX6kQODcNRLQvMjRhIFuH4Qv2.GAxlqNac4/S');
