-- name: VehicleExists :one
SELECT EXISTS(SELECT 1 FROM vehicles WHERE id = $1 AND user_id = $2);

-- name: CreateVehicleAirFill :one
INSERT INTO vehicle_air_fills (vehicle_id, user_id)
SELECT v.id, v.user_id FROM vehicles v WHERE v.id=sqlc.arg(vehicle_id)::bigint AND v.user_id=sqlc.arg(user_id)::bigint RETURNING filled_at;

-- name: ListLatestVehicleAirFills :many
SELECT DISTINCT ON (f.vehicle_id) f.vehicle_id, f.filled_at FROM vehicle_air_fills f
JOIN vehicles v ON v.id=f.vehicle_id AND v.user_id=f.user_id
WHERE f.user_id = $1 ORDER BY f.vehicle_id, f.filled_at DESC, f.id DESC;
-- name: ListDueAirFills :many
SELECT id FROM vehicle_air_fills
WHERE reminder_notification_id IS NULL AND filled_at <= NOW() - INTERVAL '30 days'
LIMIT 100;

-- name: LockDueAirFill :one
SELECT vehicle_air_fills.user_id,vehicle_air_fills.vehicle_id,vehicles.name
FROM vehicle_air_fills JOIN vehicles ON vehicles.id=vehicle_air_fills.vehicle_id
WHERE vehicle_air_fills.id=$1 AND vehicle_air_fills.reminder_notification_id IS NULL
AND vehicle_air_fills.filled_at <= NOW()-INTERVAL '30 days'
FOR UPDATE;

-- name: MarkAirFillReminderSent :exec
UPDATE vehicle_air_fills SET reminder_notification_id=$1 WHERE id=$2;
