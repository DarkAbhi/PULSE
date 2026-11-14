BEGIN;

-- Empty personal values mean an unsupported/unconfigured task, not another source.
ALTER TABLE vehicle_maintenance_schedules DROP CONSTRAINT vehicle_maintenance_schedules_check2;

WITH starting AS (
 SELECT v.id AS vehicle_id,v.user_id,k.kind,
 CASE
  WHEN k.kind='tire_pressure' THEN 0
  WHEN k.kind='engine_oil' AND v.fuel_types=ARRAY['electric']::text[] THEN 0
  WHEN k.kind='engine_oil' AND v.kind IN ('motorcycle','moped') THEN 3000
  WHEN k.kind='general_service' AND v.kind IN ('motorcycle','moped') THEN 5000
  WHEN v.kind='car' THEN 10000 ELSE 0 END AS km,
 CASE
  WHEN k.kind='tire_pressure' THEN CASE WHEN v.kind IN ('motorcycle','moped') THEN 14 ELSE 30 END
  WHEN k.kind='engine_oil' AND v.fuel_types=ARRAY['electric']::text[] THEN 0
  WHEN k.kind='engine_oil' AND v.kind IN ('motorcycle','moped') THEN 4
  WHEN k.kind='general_service' AND v.kind IN ('motorcycle','moped') THEN 6
  WHEN k.kind='engine_oil' AND v.kind='car' THEN 9
  WHEN k.kind='general_service' AND v.kind='car' THEN 12 ELSE 0 END AS duration,
 CASE WHEN k.kind='tire_pressure' THEN 'days' ELSE 'months' END AS unit
 FROM vehicles v CROSS JOIN (VALUES ('engine_oil'),('tire_pressure'),('general_service')) k(kind)
)
INSERT INTO vehicle_maintenance_schedules AS saved
 (vehicle_id,user_id,kind,active_source,personal_km,personal_duration,personal_unit)
SELECT vehicle_id,user_id,kind,'personal',km,duration,unit FROM starting
ON CONFLICT (vehicle_id,kind) DO UPDATE SET
 personal_km=EXCLUDED.personal_km,personal_duration=EXCLUDED.personal_duration,personal_unit=EXCLUDED.personal_unit
WHERE saved.personal_km=0 AND saved.personal_duration=0;

UPDATE vehicle_maintenance_schedules SET active_source='personal' WHERE active_source='default';
ALTER TABLE vehicle_maintenance_schedules DROP CONSTRAINT vehicle_maintenance_schedules_active_source_check;
ALTER TABLE vehicle_maintenance_schedules ADD CONSTRAINT vehicle_maintenance_schedules_active_source_check
 CHECK (active_source IN ('personal','manual'));

COMMIT;
