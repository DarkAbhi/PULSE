-- name: ListVehicles :many
SELECT id, name FROM vehicles ORDER BY id ASC;

-- name: CreateVehicle :one
INSERT INTO vehicles (name, is_active, front_tire_pressure_solo, rear_tire_pressure_solo, front_tire_pressure_pillion, rear_tire_pressure_pillion)
VALUES ($1, $2, $3, $4, $5, $6)
RETURNING id, name, is_active, front_tire_pressure_solo, rear_tire_pressure_solo, front_tire_pressure_pillion, rear_tire_pressure_pillion;

-- name: GetVehicle :one
SELECT id, name, is_active, front_tire_pressure_solo, rear_tire_pressure_solo,
front_tire_pressure_pillion, rear_tire_pressure_pillion FROM vehicles WHERE id=$1;

-- name: GetVehicleForUpdate :one
SELECT name, is_active, front_tire_pressure_solo, rear_tire_pressure_solo,
front_tire_pressure_pillion, rear_tire_pressure_pillion FROM vehicles WHERE id=$1;

-- name: UpdateVehicle :one
UPDATE vehicles SET name=$1, is_active=$2, front_tire_pressure_solo=$3, rear_tire_pressure_solo=$4,
front_tire_pressure_pillion=$5, rear_tire_pressure_pillion=$6, updated_at=now()
WHERE id=$7 RETURNING id, name, is_active, front_tire_pressure_solo, rear_tire_pressure_solo,
front_tire_pressure_pillion, rear_tire_pressure_pillion;

-- name: UpdateVehicleTirePressure :one
UPDATE vehicles SET front_tire_pressure_solo=$1, rear_tire_pressure_solo=$2,
front_tire_pressure_pillion=$3, rear_tire_pressure_pillion=$4, updated_at=now()
WHERE id=$5 RETURNING id, name, is_active, front_tire_pressure_solo, rear_tire_pressure_solo,
front_tire_pressure_pillion, rear_tire_pressure_pillion;

-- name: DeleteVehicle :execrows
DELETE FROM vehicles WHERE id=$1;

-- name: GetVehicleHistoryHeader :one
SELECT name, front_tire_pressure_solo, rear_tire_pressure_solo,
front_tire_pressure_pillion, rear_tire_pressure_pillion FROM vehicles WHERE id=$1;

-- name: ListVehicleAirFills :many
SELECT id,filled_at FROM vehicle_air_fills WHERE vehicle_id=$1 AND user_id=$2 ORDER BY filled_at DESC,id DESC;

-- name: ListVehicleFuelFillups :many
SELECT id,odometer_km,filled_at,station_name,notes FROM vehicle_fuel_fillups
WHERE vehicle_id=$1 AND user_id=$2 ORDER BY filled_at DESC,id DESC;

-- name: ListFuelItems :many
SELECT fuel_type,fill_type,quantity,unit_price,total_cost FROM vehicle_fuel_items WHERE fillup_id=$1 ORDER BY id;

-- name: ListVehicleMaintenanceRecords :many
SELECT id,category,title,amount,occurred_at,odometer_km,provider_name,notes
FROM vehicle_maintenance_records WHERE vehicle_id=$1 AND user_id=$2 ORDER BY occurred_at DESC,id DESC;

-- name: ListMaintenanceAttachments :many
SELECT id,file_name,content_type,size_bytes,created_at FROM vehicle_maintenance_attachments
WHERE maintenance_record_id=$1 AND user_id=$2 ORDER BY created_at ASC;

-- name: DeleteVehicleAirFill :execrows
DELETE FROM vehicle_air_fills WHERE id=$1 AND vehicle_id=$2 AND user_id=$3;

-- name: DeleteVehicleFuelFillup :execrows
DELETE FROM vehicle_fuel_fillups WHERE id=$1 AND vehicle_id=$2 AND user_id=$3;
