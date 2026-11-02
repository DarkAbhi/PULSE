CREATE TABLE vehicle_maintenance_actions (
    id BIGSERIAL PRIMARY KEY,
    maintenance_record_id BIGINT NOT NULL REFERENCES vehicle_maintenance_records(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    component VARCHAR(64) NOT NULL CHECK (component ~ '^[a-z][a-z0-9_]{0,63}$'),
    action VARCHAR(16) NOT NULL CHECK (action IN ('replaced', 'inspected', 'adjusted', 'repaired', 'topped_up', 'cleaned', 'lubricated', 'completed')),
    description VARCHAR(500) NOT NULL DEFAULT '',
    UNIQUE (maintenance_record_id, component, action),
    FOREIGN KEY (user_id, maintenance_record_id) REFERENCES vehicle_maintenance_records(user_id, id)
);
CREATE INDEX vehicle_maintenance_actions_component_idx ON vehicle_maintenance_actions (user_id, component, maintenance_record_id);
ALTER TABLE vehicle_maintenance_records DROP CONSTRAINT vehicle_maintenance_records_amount_check;
ALTER TABLE vehicle_maintenance_records ADD CONSTRAINT vehicle_maintenance_records_amount_check CHECK (amount >= 0);
