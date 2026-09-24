-- name: GetMeditationToday :one
SELECT 1 FROM meditations WHERE created_at >= $1 AND created_at < $2 LIMIT 1;

-- name: AddMeditation :exec
INSERT INTO meditations DEFAULT VALUES;

-- name: AddSport :exec
INSERT INTO sports (name) VALUES ($1);
