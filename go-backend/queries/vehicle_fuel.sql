-- name: GetMaxFuelOdometer :one
SELECT COALESCE(MAX(odometer_km), -1)::double precision FROM vehicle_fuel_fillups WHERE vehicle_id=$1;

-- name: CreateFuelFillup :one
INSERT INTO vehicle_fuel_fillups (vehicle_id,user_id,odometer_km,filled_at,station_name,notes)
VALUES ($1,$2,$3,$4,$5,$6) RETURNING id;

-- name: CreateFuelItem :exec
INSERT INTO vehicle_fuel_items (fillup_id,fuel_type,fill_type,quantity,unit_price,total_cost)
VALUES ($1,$2,$3,$4,$5,$6);

-- name: LockFuelFillup :one
SELECT 1 FROM vehicle_fuel_fillups WHERE id=$1 AND vehicle_id=$2 AND user_id=$3 FOR UPDATE;

-- name: UpdateFuelFillup :exec
UPDATE vehicle_fuel_fillups SET odometer_km=$1,filled_at=$2,station_name=$3,notes=$4 WHERE id=$5;

-- name: DeleteFuelItems :exec
DELETE FROM vehicle_fuel_items WHERE fillup_id=$1;

-- name: ListFuelEconomyEntries :many
SELECT f.odometer_km, i.fill_type, i.quantity FROM vehicle_fuel_fillups f
JOIN vehicle_fuel_items i ON i.fillup_id=f.id
WHERE f.vehicle_id=$1 AND i.fuel_type=$2 ORDER BY f.filled_at, f.id;

-- name: ListAverageFuelEconomyEntries :many
SELECT i.fuel_type, f.odometer_km, i.fill_type, i.quantity FROM vehicle_fuel_fillups f
JOIN vehicle_fuel_items i ON i.fillup_id=f.id
WHERE f.vehicle_id=$1 AND f.user_id=$2 ORDER BY i.fuel_type, f.filled_at, f.id;
