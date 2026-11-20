CREATE TABLE vehicle_maintenance_schedules (
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    kind TEXT NOT NULL CHECK (kind IN ('engine_oil','general_service','tire_pressure')),
    active_source TEXT NOT NULL CHECK (active_source IN ('default','personal','manual')),
    personal_km INTEGER NOT NULL DEFAULT 0 CHECK (personal_km BETWEEN 0 AND 1000000),
    personal_duration INTEGER NOT NULL DEFAULT 0,
    personal_unit TEXT NOT NULL DEFAULT 'months' CHECK (personal_unit IN ('days','months')),
    manual_km INTEGER NOT NULL DEFAULT 0 CHECK (manual_km BETWEEN 0 AND 1000000),
    manual_duration INTEGER NOT NULL DEFAULT 0,
    manual_unit TEXT NOT NULL DEFAULT 'months' CHECK (manual_unit IN ('days','months')),
    manual_reference VARCHAR(160) NOT NULL DEFAULT '',
    PRIMARY KEY (vehicle_id,kind),
    FOREIGN KEY (user_id,vehicle_id) REFERENCES vehicles(user_id,id) ON DELETE CASCADE,
    CHECK (personal_duration BETWEEN 0 AND CASE WHEN personal_unit='months' THEN 1200 ELSE 36500 END),
    CHECK (manual_duration BETWEEN 0 AND CASE WHEN manual_unit='months' THEN 1200 ELSE 36500 END),
    CHECK (active_source <> 'personal' OR personal_km > 0 OR personal_duration > 0),
    CHECK (active_source <> 'manual' OR manual_km > 0 OR manual_duration > 0)
);
ALTER TABLE vehicle_air_fills ADD COLUMN odometer_km NUMERIC(12,1) CHECK (odometer_km >= 0);
