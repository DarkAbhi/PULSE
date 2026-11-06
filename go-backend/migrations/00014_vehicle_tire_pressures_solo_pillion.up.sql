ALTER TABLE vehicles
    RENAME COLUMN front_tire_pressure TO front_tire_pressure_solo;

ALTER TABLE vehicles
    RENAME COLUMN rear_tire_pressure TO rear_tire_pressure_solo;

ALTER TABLE vehicles
    ADD COLUMN front_tire_pressure_pillion NUMERIC(5, 1) CHECK (front_tire_pressure_pillion IS NULL OR front_tire_pressure_pillion >= 0),
    ADD COLUMN rear_tire_pressure_pillion NUMERIC(5, 1) CHECK (rear_tire_pressure_pillion IS NULL OR rear_tire_pressure_pillion >= 0);
