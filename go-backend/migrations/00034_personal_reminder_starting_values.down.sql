BEGIN;
ALTER TABLE vehicle_maintenance_schedules DROP CONSTRAINT vehicle_maintenance_schedules_active_source_check;
ALTER TABLE vehicle_maintenance_schedules ADD CONSTRAINT vehicle_maintenance_schedules_active_source_check
 CHECK (active_source IN ('default','personal','manual'));
UPDATE vehicle_maintenance_schedules SET active_source='default'
 WHERE active_source='personal' AND personal_km=0 AND personal_duration=0;
ALTER TABLE vehicle_maintenance_schedules ADD CONSTRAINT vehicle_maintenance_schedules_check2
 CHECK (active_source <> 'personal' OR personal_km > 0 OR personal_duration > 0);
-- Keep seeded/user-edited values: their origins cannot safely be distinguished.
COMMIT;
