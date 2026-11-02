-- name: VehicleExists :one
SELECT EXISTS(SELECT 1 FROM vehicles WHERE id = $1 AND user_id = $2);

-- name: CreateVehicleAirFill :one
INSERT INTO vehicle_air_fills (vehicle_id, user_id, odometer_km)
SELECT v.id, v.user_id, sqlc.narg(odometer_km)::numeric FROM vehicles v WHERE v.id=sqlc.arg(vehicle_id)::bigint AND v.user_id=sqlc.arg(user_id)::bigint RETURNING filled_at;

-- name: ListDueAirFills :many
SELECT a.id
FROM vehicle_air_fills a JOIN vehicles v ON v.id=a.vehicle_id AND v.user_id=a.user_id
LEFT JOIN vehicle_maintenance_schedules s ON s.vehicle_id=v.id AND s.user_id=v.user_id AND s.kind='tire_pressure'
CROSS JOIN LATERAL (
 SELECT CASE s.active_source WHEN 'personal' THEN s.personal_km WHEN 'manual' THEN s.manual_km ELSE 0 END AS km,
 CASE s.active_source WHEN 'personal' THEN s.personal_duration WHEN 'manual' THEN s.manual_duration ELSE CASE WHEN v.kind IN ('motorcycle','moped') THEN 14 ELSE 30 END END AS duration,
 CASE s.active_source WHEN 'personal' THEN s.personal_unit WHEN 'manual' THEN s.manual_unit ELSE 'days' END AS unit
) i
LEFT JOIN LATERAL (
 SELECT readings.odometer_km FROM (
  SELECT odometer_km,filled_at AS recorded_at FROM vehicle_fuel_fillups WHERE vehicle_id=a.vehicle_id AND user_id=a.user_id
  UNION ALL SELECT odometer_km,occurred_at FROM vehicle_maintenance_records WHERE vehicle_id=a.vehicle_id AND user_id=a.user_id
  UNION ALL SELECT odometer_km,filled_at FROM vehicle_air_fills WHERE vehicle_id=a.vehicle_id AND user_id=a.user_id
 ) readings WHERE odometer_km IS NOT NULL AND recorded_at<=NOW() ORDER BY recorded_at DESC,odometer_km DESC LIMIT 1
) mileage ON TRUE
WHERE a.reminder_notification_id IS NULL AND a.filled_at<=NOW()
AND NOT EXISTS (SELECT 1 FROM vehicle_air_fills newer WHERE newer.vehicle_id=a.vehicle_id AND (newer.filled_at,newer.id)>(a.filled_at,a.id))
AND (
 (i.duration>0 AND ((a.filled_at AT TIME ZONE 'Asia/Kolkata')::date + CASE WHEN i.unit='months' THEN make_interval(months=>i.duration) ELSE make_interval(days=>i.duration) END)::date <= (NOW() AT TIME ZONE 'Asia/Kolkata')::date)
 OR (i.km>0 AND a.odometer_km IS NOT NULL AND mileage.odometer_km>=a.odometer_km+i.km)
)
ORDER BY a.filled_at,a.id LIMIT 100;

-- name: LockDueAirFill :one
SELECT a.user_id,a.vehicle_id,v.name
FROM vehicle_air_fills a JOIN vehicles v ON v.id=a.vehicle_id AND v.user_id=a.user_id
LEFT JOIN vehicle_maintenance_schedules s ON s.vehicle_id=v.id AND s.user_id=v.user_id AND s.kind='tire_pressure'
CROSS JOIN LATERAL (
 SELECT CASE s.active_source WHEN 'personal' THEN s.personal_km WHEN 'manual' THEN s.manual_km ELSE 0 END AS km,
 CASE s.active_source WHEN 'personal' THEN s.personal_duration WHEN 'manual' THEN s.manual_duration ELSE CASE WHEN v.kind IN ('motorcycle','moped') THEN 14 ELSE 30 END END AS duration,
 CASE s.active_source WHEN 'personal' THEN s.personal_unit WHEN 'manual' THEN s.manual_unit ELSE 'days' END AS unit
) i
LEFT JOIN LATERAL (
 SELECT readings.odometer_km FROM (
  SELECT odometer_km,filled_at AS recorded_at FROM vehicle_fuel_fillups WHERE vehicle_id=a.vehicle_id AND user_id=a.user_id
  UNION ALL SELECT odometer_km,occurred_at FROM vehicle_maintenance_records WHERE vehicle_id=a.vehicle_id AND user_id=a.user_id
  UNION ALL SELECT odometer_km,filled_at FROM vehicle_air_fills WHERE vehicle_id=a.vehicle_id AND user_id=a.user_id
 ) readings WHERE odometer_km IS NOT NULL AND recorded_at<=NOW() ORDER BY recorded_at DESC,odometer_km DESC LIMIT 1
) mileage ON TRUE
WHERE a.id=$1 AND a.reminder_notification_id IS NULL AND a.filled_at<=NOW()
AND NOT EXISTS (SELECT 1 FROM vehicle_air_fills newer WHERE newer.vehicle_id=a.vehicle_id AND (newer.filled_at,newer.id)>(a.filled_at,a.id))
AND (
 (i.duration>0 AND ((a.filled_at AT TIME ZONE 'Asia/Kolkata')::date + CASE WHEN i.unit='months' THEN make_interval(months=>i.duration) ELSE make_interval(days=>i.duration) END)::date <= (NOW() AT TIME ZONE 'Asia/Kolkata')::date)
 OR (i.km>0 AND a.odometer_km IS NOT NULL AND mileage.odometer_km>=a.odometer_km+i.km)
)
FOR UPDATE OF a;

-- name: MarkAirFillReminderSent :exec
UPDATE vehicle_air_fills SET reminder_notification_id=$1 WHERE id=$2;
