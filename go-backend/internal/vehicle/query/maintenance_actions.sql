-- name: CreateMaintenanceAction :exec
INSERT INTO vehicle_maintenance_actions (maintenance_record_id, user_id, component, action, description)
VALUES ($1, $2, $3, $4, $5);

-- name: DeleteMaintenanceActions :exec
DELETE FROM vehicle_maintenance_actions WHERE maintenance_record_id=$1 AND user_id=$2;

-- name: ListMaintenanceActions :many
SELECT component, action, description FROM vehicle_maintenance_actions
WHERE maintenance_record_id=$1 AND user_id=$2 ORDER BY id;

-- name: SearchMaintenanceHistory :many
SELECT r.id, r.category, r.title, r.amount, r.occurred_at, r.odometer_km, r.provider_name, r.notes
FROM vehicle_maintenance_records r
WHERE r.vehicle_id=sqlc.arg(vehicle_id) AND r.user_id=sqlc.arg(user_id)
AND r.occurred_at <= NOW()
AND EXISTS (
    SELECT 1 FROM vehicle_maintenance_actions a WHERE a.maintenance_record_id=r.id AND a.user_id=r.user_id
    AND a.component=sqlc.arg(component)::varchar
    AND (sqlc.arg(action)::varchar='' OR a.action=sqlc.arg(action)::varchar)
)
ORDER BY r.occurred_at DESC, r.id DESC LIMIT 50;
