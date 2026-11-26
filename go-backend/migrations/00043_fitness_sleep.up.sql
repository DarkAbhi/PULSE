CREATE TABLE fitness_sleep (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sleep_date DATE NOT NULL,
    total_asleep_seconds INTEGER NOT NULL CHECK (total_asleep_seconds BETWEEN 0 AND 86400),
    main_sleep_seconds INTEGER NOT NULL CHECK (main_sleep_seconds BETWEEN 0 AND total_asleep_seconds),
    wake_at TIMESTAMPTZ NOT NULL,
    sessions JSONB NOT NULL CHECK (jsonb_typeof(sessions) = 'array' AND sessions <> '[]'::jsonb),
    UNIQUE (user_id, sleep_date)
);
