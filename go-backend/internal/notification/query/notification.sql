-- name: ListNotifications :many
SELECT id,source,title,body,target_path,priority,created_at FROM notifications
WHERE user_id=$1 AND dismissed_at IS NULL ORDER BY created_at DESC,id DESC LIMIT $2;

-- name: DismissNotification :execrows
UPDATE notifications SET dismissed_at=NOW() WHERE id=$1 AND user_id=$2 AND dismissed_at IS NULL;

-- name: ClearNotifications :exec
UPDATE notifications SET dismissed_at=NOW() WHERE user_id=$1 AND dismissed_at IS NULL;

-- name: CreateAirFillReminder :one
INSERT INTO notifications (user_id,source,title,body,target_path,priority,metadata)
VALUES ($1,'Garage','Time to check ' || $3 || '''s air','Your tire pressure check is due for ' || $3 || '. Check the tires when cold.','/garage',1,jsonb_build_object('vehicle_id',$2::bigint))
RETURNING id;
