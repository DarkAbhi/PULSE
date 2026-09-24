-- name: ListActiveNotifications :many
SELECT id, source, title, body, target_path, priority, created_at
FROM notifications
WHERE user_id = $1 AND dismissed_at IS NULL
ORDER BY created_at DESC, id DESC
LIMIT $2;

-- name: DismissNotification :execrows
UPDATE notifications SET dismissed_at = NOW()
WHERE id = $1 AND user_id = $2 AND dismissed_at IS NULL;

-- name: DismissAllNotifications :exec
UPDATE notifications SET dismissed_at = NOW()
WHERE user_id = $1 AND dismissed_at IS NULL;
