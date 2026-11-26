ALTER TABLE vehicles
    DROP COLUMN IF EXISTS front_tire_pressure_pillion,
    DROP COLUMN IF EXISTS rear_tire_pressure_pillion;

ALTER TABLE vehicles
    RENAME COLUMN front_tire_pressure_solo TO front_tire_pressure;

ALTER TABLE vehicles
    RENAME COLUMN rear_tire_pressure_solo TO rear_tire_pressure;
