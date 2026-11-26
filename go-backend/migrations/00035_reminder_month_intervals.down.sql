BEGIN;
ALTER TABLE vehicle_maintenance_schedules DROP CONSTRAINT vehicle_maintenance_schedules_personal_unit_check;
ALTER TABLE vehicle_maintenance_schedules DROP CONSTRAINT vehicle_maintenance_schedules_manual_unit_check;
ALTER TABLE vehicle_maintenance_schedules ADD CONSTRAINT vehicle_maintenance_schedules_personal_unit_check CHECK (personal_unit IN ('days','months'));
ALTER TABLE vehicle_maintenance_schedules ADD CONSTRAINT vehicle_maintenance_schedules_manual_unit_check CHECK (manual_unit IN ('days','months'));
-- Keep converted/user-edited months: the original day values cannot be recovered safely.
COMMIT;
