CREATE TABLE vehicle_maintenance_records (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(16) NOT NULL CHECK (category IN ('service', 'repair', 'insurance', 'washing', 'tyres')),
    title VARCHAR(160) NOT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    odometer_km NUMERIC(12,1) CHECK (odometer_km >= 0),
    provider_name VARCHAR(160),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX vehicle_maintenance_records_history_idx
    ON vehicle_maintenance_records (vehicle_id, user_id, occurred_at DESC, id DESC);
