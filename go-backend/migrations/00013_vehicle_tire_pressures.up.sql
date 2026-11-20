ALTER TABLE vehicles
ADD COLUMN front_tire_pressure NUMERIC(5, 1) CHECK (front_tire_pressure IS NULL OR front_tire_pressure >= 0),
ADD COLUMN rear_tire_pressure NUMERIC(5, 1) CHECK (rear_tire_pressure IS NULL OR rear_tire_pressure >= 0);
