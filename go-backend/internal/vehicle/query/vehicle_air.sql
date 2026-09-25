-- name: VehicleExists :one
SELECT EXISTS(SELECT 1 FROM vehicles WHERE id = $1);

-- name: CreateVehicleAirFill :one
INSERT INTO vehicle_air_fills (vehicle_id, user_id) VALUES ($1, $2) RETURNING filled_at;

-- name: ListLatestVehicleAirFills :many
SELECT DISTINCT ON (vehicle_id) vehicle_id, filled_at FROM vehicle_air_fills
WHERE user_id = $1 ORDER BY vehicle_id, filled_at DESC, id DESC;

