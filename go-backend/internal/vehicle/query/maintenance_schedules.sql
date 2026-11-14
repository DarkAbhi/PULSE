-- name: ListMaintenanceSchedules :many
SELECT kind,active_source,personal_km,personal_duration,personal_unit,manual_km,manual_duration,manual_unit,manual_reference
FROM vehicle_maintenance_schedules WHERE vehicle_id=$1 AND user_id=$2 ORDER BY kind;

-- name: SaveMaintenanceSchedule :execrows
INSERT INTO vehicle_maintenance_schedules (vehicle_id,user_id,kind,active_source,personal_km,personal_duration,personal_unit,manual_km,manual_duration,manual_unit,manual_reference)
SELECT id,user_id,sqlc.arg(kind)::text,sqlc.arg(active_source)::text,
 sqlc.arg(personal_km)::integer,sqlc.arg(personal_duration)::integer,sqlc.arg(personal_unit)::text,
 sqlc.arg(manual_km)::integer,sqlc.arg(manual_duration)::integer,sqlc.arg(manual_unit)::text,sqlc.arg(manual_reference)::varchar
FROM vehicles WHERE id=sqlc.arg(vehicle_id)::bigint AND user_id=sqlc.arg(user_id)::bigint
ON CONFLICT (vehicle_id,kind) DO UPDATE SET active_source=EXCLUDED.active_source,
 personal_km=EXCLUDED.personal_km,personal_duration=EXCLUDED.personal_duration,personal_unit=EXCLUDED.personal_unit,
 manual_km=EXCLUDED.manual_km,manual_duration=EXCLUDED.manual_duration,manual_unit=EXCLUDED.manual_unit,manual_reference=EXCLUDED.manual_reference;

-- name: DismissPressureScheduleAlerts :exec
UPDATE notifications n SET dismissed_at=NOW()
WHERE n.user_id=sqlc.arg(user_id)::bigint AND n.dismissed_at IS NULL AND n.id IN (
 SELECT a.reminder_notification_id FROM vehicle_air_fills a WHERE a.vehicle_id=sqlc.arg(vehicle_id)::bigint AND a.user_id=sqlc.arg(user_id)::bigint
);

-- name: ResetLatestPressureReminder :exec
UPDATE vehicle_air_fills a SET reminder_notification_id=NULL
WHERE a.id=(SELECT f.id FROM vehicle_air_fills f WHERE f.vehicle_id=sqlc.arg(vehicle_id)::bigint AND f.user_id=sqlc.arg(user_id)::bigint ORDER BY f.filled_at DESC,f.id DESC LIMIT 1);
