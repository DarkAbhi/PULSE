-- name: CreateMaintenanceAttachment :one
INSERT INTO vehicle_maintenance_attachments (maintenance_record_id,user_id,storage_key,file_name,content_type,size_bytes)
VALUES ($1,$2,$3,$4,$5,$6) RETURNING id,file_name,content_type,size_bytes,created_at;

-- name: GetMaintenanceAttachmentKey :one
SELECT a.storage_key FROM vehicle_maintenance_attachments a
JOIN vehicle_maintenance_records r ON r.id=a.maintenance_record_id
WHERE a.id=$1 AND a.maintenance_record_id=$2 AND r.vehicle_id=$3 AND a.user_id=$4;

-- name: DeleteMaintenanceAttachment :exec
DELETE FROM vehicle_maintenance_attachments WHERE id=$1 AND user_id=$2;

-- name: ListMaintenanceAttachmentKeys :many
SELECT storage_key FROM vehicle_maintenance_attachments WHERE maintenance_record_id=$1 AND user_id=$2;

-- name: OwnsMaintenanceRecord :one
SELECT EXISTS(SELECT 1 FROM vehicle_maintenance_records WHERE id=$1 AND vehicle_id=$2 AND user_id=$3);
