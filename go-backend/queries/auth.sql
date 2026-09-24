-- name: GetLoginUser :one
SELECT id, password_hash FROM users WHERE username = $1;

-- name: CreateSession :exec
INSERT INTO user_sessions (user_id, token_hash, expires_at) VALUES ($1, $2, $3);

-- name: DeleteSession :exec
DELETE FROM user_sessions WHERE token_hash = $1;

-- name: GetSessionUser :one
SELECT users.id, users.username
FROM user_sessions
JOIN users ON users.id = user_sessions.user_id
WHERE user_sessions.token_hash = $1 AND user_sessions.expires_at > NOW();
