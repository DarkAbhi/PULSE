BEGIN;
CREATE TEMP TABLE month_schedule_alerts (vehicle_id BIGINT,user_id BIGINT) ON COMMIT DROP;
INSERT INTO month_schedule_alerts
SELECT vehicle_id,user_id FROM vehicle_maintenance_schedules
WHERE kind='tire_pressure' AND (
 (active_source='personal' AND personal_unit='days' AND personal_duration>0)
 OR (active_source='manual' AND manual_unit='days' AND manual_duration>0)
);

-- Approximate day-based intervals in whole months, rounding up; preserve blank limits.
UPDATE vehicle_maintenance_schedules SET personal_duration=(personal_duration*12+364)/365,personal_unit='months'
WHERE personal_unit='days';
UPDATE vehicle_maintenance_schedules SET manual_duration=(manual_duration*12+364)/365,manual_unit='months'
WHERE manual_unit='days';
ALTER TABLE vehicle_maintenance_schedules DROP CONSTRAINT vehicle_maintenance_schedules_personal_unit_check;
ALTER TABLE vehicle_maintenance_schedules DROP CONSTRAINT vehicle_maintenance_schedules_manual_unit_check;
ALTER TABLE vehicle_maintenance_schedules ADD CONSTRAINT vehicle_maintenance_schedules_personal_unit_check CHECK (personal_unit='months');
ALTER TABLE vehicle_maintenance_schedules ADD CONSTRAINT vehicle_maintenance_schedules_manual_unit_check CHECK (manual_unit='months');

UPDATE notifications n SET dismissed_at=NOW()
FROM vehicle_air_fills a JOIN month_schedule_alerts changed ON changed.vehicle_id=a.vehicle_id AND changed.user_id=a.user_id
WHERE n.id=a.reminder_notification_id AND n.user_id=changed.user_id AND n.dismissed_at IS NULL;
UPDATE vehicle_air_fills a SET reminder_notification_id=NULL
FROM month_schedule_alerts changed
WHERE a.user_id=changed.user_id AND a.id=(
 SELECT latest.id FROM vehicle_air_fills latest WHERE latest.vehicle_id=changed.vehicle_id AND latest.user_id=changed.user_id
 ORDER BY latest.filled_at DESC,latest.id DESC LIMIT 1
);
COMMIT;
