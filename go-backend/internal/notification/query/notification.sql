-- name: ListNotifications :many
SELECT id,source,title,body,target_path,priority,created_at FROM notifications
WHERE user_id=$1 AND dismissed_at IS NULL ORDER BY created_at DESC,id DESC LIMIT $2;

-- name: DismissNotification :execrows
UPDATE notifications SET dismissed_at=NOW() WHERE id=$1 AND user_id=$2 AND dismissed_at IS NULL;

-- name: ClearNotifications :exec
UPDATE notifications SET dismissed_at=NOW() WHERE user_id=$1 AND dismissed_at IS NULL;

-- name: CreateGymReminder :one
INSERT INTO notifications (user_id,source,title,body,target_path,priority,metadata)
VALUES ($1,'Gym reminder','Time for the gym','Your 3:30 PM gym reminder. Mark your visit when you are done.','/gym-visits',1,jsonb_build_object('reminder_date',$2::text))
RETURNING id;

-- name: GymReminderExists :one
SELECT EXISTS(SELECT 1 FROM notifications WHERE id=$1 AND user_id=$2 AND source='Gym reminder' AND dismissed_at IS NULL);

-- name: DismissGymReminder :exec
UPDATE notifications SET dismissed_at=NOW() WHERE id=$1;

-- name: CreateAirFillReminder :one
INSERT INTO notifications (user_id,source,title,body,target_path,priority,metadata)
VALUES ($1,'Garage','Time to check ' || $3 || '''s air','It has been 30 days since you last filled air in ' || $3 || '.','/garage',1,jsonb_build_object('vehicle_id',$2::bigint))
RETURNING id;
