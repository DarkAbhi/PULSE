-- name: VehicleExists :one
SELECT EXISTS(SELECT 1 FROM vehicles WHERE id = $1 AND user_id = $2);

-- name: CreateVehicleAirFill :one
INSERT INTO vehicle_air_fills (vehicle_id, user_id)
SELECT v.id, v.user_id FROM vehicles v WHERE v.id=sqlc.arg(vehicle_id)::bigint AND v.user_id=sqlc.arg(user_id)::bigint RETURNING filled_at;

-- name: ListDueAirFills :many
SELECT a.id FROM vehicle_air_fills a JOIN vehicles v ON v.id=a.vehicle_id AND v.user_id=a.user_id
WHERE a.reminder_notification_id IS NULL
AND a.filled_at <= NOW() - CASE WHEN v.kind IN ('motorcycle','moped') THEN INTERVAL '14 days' ELSE INTERVAL '30 days' END
AND NOT EXISTS (SELECT 1 FROM vehicle_air_fills newer WHERE newer.vehicle_id=a.vehicle_id AND (newer.filled_at, newer.id) > (a.filled_at, a.id))
LIMIT 100;

-- name: LockDueAirFill :one
SELECT vehicle_air_fills.user_id,vehicle_air_fills.vehicle_id,vehicles.name
FROM vehicle_air_fills JOIN vehicles ON vehicles.id=vehicle_air_fills.vehicle_id
WHERE vehicle_air_fills.id=$1 AND vehicle_air_fills.reminder_notification_id IS NULL
AND vehicle_air_fills.filled_at <= NOW() - CASE WHEN vehicles.kind IN ('motorcycle','moped') THEN INTERVAL '14 days' ELSE INTERVAL '30 days' END
AND NOT EXISTS (SELECT 1 FROM vehicle_air_fills newer WHERE newer.vehicle_id=vehicle_air_fills.vehicle_id AND (newer.filled_at, newer.id) > (vehicle_air_fills.filled_at, vehicle_air_fills.id))
FOR UPDATE OF vehicle_air_fills;

-- name: MarkAirFillReminderSent :exec
UPDATE vehicle_air_fills SET reminder_notification_id=$1 WHERE id=$2;
