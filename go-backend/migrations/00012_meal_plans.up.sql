CREATE TABLE meal_times (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX meal_times_user_name_idx ON meal_times (COALESCE(user_id, 0), LOWER(name));

INSERT INTO meal_times (name, start_time, end_time, is_default) VALUES
    ('Breakfast', '07:00:00', '10:00:00', true),
    ('Lunch', '12:00:00', '14:30:00', true),
    ('Evening Snacks', '16:30:00', '18:30:00', true),
    ('Dinner', '19:30:00', '22:00:00', true)
ON CONFLICT DO NOTHING;

CREATE TABLE meal_plans (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    name VARCHAR(255) NOT NULL,
    meal_time_id BIGINT NULL REFERENCES meal_times(id) ON DELETE SET NULL,
    start_time TIME NULL,
    end_time TIME NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX meal_plans_user_date_idx ON meal_plans (user_id, date, created_at ASC);
CREATE INDEX meal_plans_meal_time_idx ON meal_plans (meal_time_id);
