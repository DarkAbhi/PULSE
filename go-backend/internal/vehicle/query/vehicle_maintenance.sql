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
SET category=sqlc.arg(category)::varchar,title=sqlc.arg(title)::varchar,amount=sqlc.arg(amount)::numeric,
 occurred_at=COALESCE(sqlc.narg(occurred_at)::timestamptz,occurred_at),odometer_km=sqlc.narg(odometer_km)::numeric,
 provider_name=sqlc.narg(provider_name)::varchar,notes=sqlc.narg(notes)::text,updated_at=CURRENT_TIMESTAMP
WHERE id=sqlc.arg(id)::bigint AND vehicle_id=sqlc.arg(vehicle_id)::bigint AND user_id=sqlc.arg(user_id)::bigint
RETURNING id,category,title,amount,occurred_at,odometer_km,provider_name,notes;

-- name: DeleteMaintenanceRecord :execrows
DELETE FROM vehicle_maintenance_records WHERE id=$1 AND vehicle_id=$2 AND user_id=$3;
