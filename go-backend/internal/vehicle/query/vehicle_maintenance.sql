-- name: CreateMaintenanceRecord :one
INSERT INTO vehicle_maintenance_records (vehicle_id,user_id,category,title,amount,occurred_at,odometer_km,provider_name,notes)
VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9)
RETURNING id,category,title,amount,occurred_at,odometer_km,provider_name,notes;

-- name: UpdateMaintenanceRecord :one
UPDATE vehicle_maintenance_records
SET category=$1,title=$2,amount=$3,occurred_at=$4,odometer_km=$5,provider_name=$6,notes=$7,updated_at=CURRENT_TIMESTAMP
WHERE id=$8 AND vehicle_id=$9 AND user_id=$10
RETURNING id,category,title,amount,occurred_at,odometer_km,provider_name,notes;

-- name: DeleteMaintenanceRecord :execrows
DELETE FROM vehicle_maintenance_records WHERE id=$1 AND vehicle_id=$2 AND user_id=$3;
