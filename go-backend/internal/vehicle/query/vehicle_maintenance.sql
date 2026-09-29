-- name: CreateMaintenanceRecord :one
INSERT INTO vehicle_maintenance_records (vehicle_id,user_id,category,title,amount,occurred_at,odometer_km,provider_name,notes)
SELECT v.id,v.user_id,
       sqlc.arg(category)::varchar, sqlc.arg(title)::varchar, sqlc.arg(amount)::numeric,
       sqlc.arg(occurred_at)::timestamptz, sqlc.narg(odometer_km)::numeric,
       sqlc.narg(provider_name)::varchar, sqlc.narg(notes)::text
FROM vehicles v WHERE v.id=sqlc.arg(vehicle_id)::bigint AND v.user_id=sqlc.arg(user_id)::bigint
RETURNING id,category,title,amount,occurred_at,odometer_km,provider_name,notes;

-- name: UpdateMaintenanceRecord :one
UPDATE vehicle_maintenance_records
SET category=$1,title=$2,amount=$3,occurred_at=$4,odometer_km=$5,provider_name=$6,notes=$7,updated_at=CURRENT_TIMESTAMP
WHERE id=$8 AND vehicle_id=$9 AND user_id=$10
RETURNING id,category,title,amount,occurred_at,odometer_km,provider_name,notes;

-- name: DeleteMaintenanceRecord :execrows
DELETE FROM vehicle_maintenance_records WHERE id=$1 AND vehicle_id=$2 AND user_id=$3;
