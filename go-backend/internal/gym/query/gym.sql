-- name: VisitToday :one
SELECT id FROM gym_visits WHERE created_at >= $1 AND created_at < $2 ORDER BY created_at DESC LIMIT 1;

-- name: AddVisit :one
INSERT INTO gym_visits DEFAULT VALUES RETURNING id;

-- name: ListVisits :many
SELECT id,created_at FROM gym_visits ORDER BY created_at DESC,id DESC;

-- name: DeleteVisit :execrows
DELETE FROM gym_visits WHERE id=$1;

-- name: VisitExists :one
SELECT EXISTS(SELECT 1 FROM gym_visits WHERE id=$1);

-- name: ListExercises :many
SELECT id,name FROM gym_visit_exercises WHERE gym_visit_id=$1 ORDER BY id ASC;

-- name: ListSets :many
SELECT id,set_number,reps,weight FROM gym_exercise_sets WHERE gym_visit_exercise_id=$1 ORDER BY set_number ASC;

-- name: CreateExercise :one
INSERT INTO gym_visit_exercises (gym_visit_id,name) VALUES ($1,$2) RETURNING id,name;

-- name: CreateSet :one
INSERT INTO gym_exercise_sets (gym_visit_exercise_id,set_number,reps,weight)
VALUES ($1,$2,$3,$4) RETURNING id,set_number,reps,weight;

-- name: DeliveryExists :one
SELECT EXISTS(SELECT 1 FROM gym_reminder_deliveries WHERE user_id=$1 AND reminder_date=($2::text)::date);

-- name: RecordDelivery :exec
INSERT INTO gym_reminder_deliveries (user_id,reminder_date,notification_id)
VALUES ($1,($2::text)::date,$3);
