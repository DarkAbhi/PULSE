-- name: LoginUser :one
SELECT id,password_hash FROM users WHERE username=$1;

-- name: CreateSession :exec
INSERT INTO user_sessions (user_id,token_hash,expires_at) VALUES ($1,$2,$3);

-- name: DeleteSession :exec
DELETE FROM user_sessions WHERE token_hash=$1;

-- name: SessionUser :one
SELECT users.id,users.username FROM user_sessions
JOIN users ON users.id=user_sessions.user_id
WHERE user_sessions.token_hash=$1 AND user_sessions.expires_at>NOW();

-- name: PasswordHash :one
SELECT password_hash FROM users WHERE id=$1;

-- name: UpdatePasswordHash :exec
UPDATE users SET password_hash=$1,updated_at=NOW() WHERE id=$2;

-- name: ListUserIDs :many
SELECT id FROM users;
