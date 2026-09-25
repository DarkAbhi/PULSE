-- name: VehicleExists :one
SELECT EXISTS(SELECT 1 FROM vehicles WHERE id = $1);

-- name: CreateVehicleAirFill :one
INSERT INTO vehicle_air_fills (vehicle_id, user_id) VALUES ($1, $2) RETURNING filled_at;

-- name: ListLatestVehicleAirFills :many
SELECT DISTINCT ON (vehicle_id) vehicle_id, filled_at FROM vehicle_air_fills
WHERE user_id = $1 ORDER BY vehicle_id, filled_at DESC, id DESC;
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
