CREATE TABLE vehicle_maintenance_attachments (
    id BIGSERIAL PRIMARY KEY,
    maintenance_record_id BIGINT NOT NULL REFERENCES vehicle_maintenance_records(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    storage_key TEXT NOT NULL UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(127) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX vehicle_maintenance_attachments_record_idx
    ON vehicle_maintenance_attachments (maintenance_record_id, created_at ASC);
