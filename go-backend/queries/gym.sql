-- name: GetGymVisitToday :one
SELECT id FROM gym_visits WHERE created_at >= $1 AND created_at < $2
ORDER BY created_at DESC LIMIT 1;

-- name: CreateGymVisit :one
INSERT INTO gym_visits DEFAULT VALUES RETURNING id;

-- name: ListGymVisits :many
SELECT id, created_at FROM gym_visits ORDER BY created_at DESC, id DESC;

-- name: DeleteGymVisit :execrows
DELETE FROM gym_visits WHERE id = $1;

-- name: ListGymVisitExercises :many
SELECT id, name FROM gym_visit_exercises WHERE gym_visit_id = $1 ORDER BY id ASC;

-- name: CreateGymVisitExercise :one
INSERT INTO gym_visit_exercises (gym_visit_id, name) VALUES ($1, $2) RETURNING id, name;

-- name: CreateGymExerciseSet :one
INSERT INTO gym_exercise_sets (gym_visit_exercise_id, set_number, reps, weight)
VALUES ($1, $2, $3, $4) RETURNING id, set_number, reps, weight;

-- name: GymReminderExists :one
SELECT EXISTS(SELECT 1 FROM notifications
WHERE id = $1 AND user_id = $2 AND source = $3 AND dismissed_at IS NULL);

-- name: DismissGymReminder :exec
UPDATE notifications SET dismissed_at = NOW() WHERE id = $1;

-- name: GymVisitExists :one
SELECT 1 FROM gym_visits WHERE id = $1;

-- name: ListGymExerciseSets :many
SELECT id, set_number, reps, weight FROM gym_exercise_sets
WHERE gym_visit_exercise_id = $1 ORDER BY set_number ASC;

-- name: ListGymReminderUsers :many
SELECT id FROM users;

-- name: CreateGymReminderNotification :one
INSERT INTO notifications (user_id, source, title, body, target_path, priority, metadata)
SELECT $1, $2, 'Time for the gym', 'Your 3:30 PM gym reminder. Mark your visit when you are done.', '/gym-visits', 1, jsonb_build_object('reminder_date', $3::text)
WHERE NOT EXISTS (
    SELECT 1 FROM gym_reminder_deliveries WHERE user_id = $1 AND reminder_date = ($3::text)::date
)
RETURNING id;

-- name: RecordGymReminderDelivery :exec
INSERT INTO gym_reminder_deliveries (user_id, reminder_date, notification_id)
VALUES ($1, ($2::text)::date, $3);
