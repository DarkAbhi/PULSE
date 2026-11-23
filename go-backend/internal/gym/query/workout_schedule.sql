-- name: UpdateWorkoutClassification :execrows
UPDATE gym_visits
SET workout_type = sqlc.narg(workout_type), workout_focus = sqlc.narg(workout_focus), updated_at = NOW()
WHERE id = sqlc.arg(visit_id) AND user_id = sqlc.arg(user_id);

-- name: ListWorkoutSchedule :many
SELECT weekday, workout_type, workout_focus
FROM fitness_workout_schedule
WHERE user_id = $1
ORDER BY weekday;

-- name: UpsertWorkoutScheduleDay :exec
INSERT INTO fitness_workout_schedule (user_id, weekday, workout_type, workout_focus)
VALUES ($1, $2, $3, $4)
ON CONFLICT (user_id, weekday) DO UPDATE
SET workout_type = EXCLUDED.workout_type, workout_focus = EXCLUDED.workout_focus;

-- name: DeleteWorkoutScheduleDay :exec
DELETE FROM fitness_workout_schedule
WHERE user_id = $1 AND weekday = $2;
