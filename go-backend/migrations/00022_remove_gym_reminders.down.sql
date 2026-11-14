CREATE TABLE gym_reminder_deliveries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reminder_date DATE NOT NULL,
    notification_id BIGINT NOT NULL UNIQUE REFERENCES notifications(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, reminder_date)
);

CREATE INDEX gym_reminder_deliveries_date_idx ON gym_reminder_deliveries (reminder_date);
