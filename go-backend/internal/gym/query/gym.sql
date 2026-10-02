-- name: VisitToday :one
SELECT id FROM gym_visits WHERE user_id=$1 AND created_at >= $2 AND created_at < $3 ORDER BY created_at DESC LIMIT 1;

-- name: AddVisit :one
INSERT INTO gym_visits (user_id) VALUES ($1) RETURNING id;

-- name: AddVisitOnDate :one
INSERT INTO gym_visits (user_id, created_at) VALUES ($1, $2) RETURNING id;

-- name: ListVisits :many
SELECT id,created_at FROM gym_visits WHERE user_id=$1 ORDER BY created_at DESC,id DESC;

-- name: OverviewVisits :many
SELECT g.id, g.created_at,
    COUNT(s.id)::bigint AS sets,
    COALESCE(SUM(s.reps * s.weight), 0)::double precision AS volume,
    COUNT(s.id) FILTER (WHERE s.weight IS NULL)::bigint AS unweighted_sets
FROM gym_visits g
LEFT JOIN gym_visit_exercises e ON e.gym_visit_id = g.id
LEFT JOIN gym_exercise_sets s ON s.gym_visit_exercise_id = e.id
WHERE g.user_id = $1 AND g.created_at <= $2
GROUP BY g.id
ORDER BY g.created_at DESC, g.id DESC;

-- name: OverviewSessions :many
SELECT w.start_time, w.duration_seconds::double precision AS duration_seconds
FROM fitness_workouts w
JOIN workout_activity_types a ON a.id = w.activity_type_id
WHERE w.user_id = $1 AND a.raw_value = 50
    AND w.start_time >= $2 AND w.start_time <= $3
    AND EXISTS (
        SELECT 1 FROM gym_visits g WHERE g.user_id = w.user_id AND g.created_at <= $3
        AND (g.created_at AT TIME ZONE 'Asia/Kolkata')::date =
            (w.start_time AT TIME ZONE 'Asia/Kolkata')::date
    );

-- name: GetVisit :one
SELECT g.id, g.created_at, w.start_time, w.end_time,
    w.duration_seconds, w.calories_burned
FROM gym_visits AS g
LEFT JOIN fitness_workouts AS w ON w.id = g.fitness_workout_id AND w.user_id = g.user_id
WHERE g.id = $1 AND g.user_id = $2;

-- name: DeleteVisit :execrows
DELETE FROM gym_visits WHERE id=$1 AND user_id=$2;

-- name: VisitExists :one
SELECT EXISTS(SELECT 1 FROM gym_visits WHERE id=$1 AND user_id=$2);

-- name: ListExercises :many
SELECT id,name FROM gym_visit_exercises WHERE gym_visit_id=$1 ORDER BY id ASC;

-- name: ListSets :many
SELECT id,set_number,reps,weight FROM gym_exercise_sets WHERE gym_visit_exercise_id=$1 ORDER BY set_number ASC;

-- name: CreateExercise :one
INSERT INTO gym_visit_exercises (gym_visit_id,name) VALUES ($1,$2) RETURNING id,name;

-- name: CreateSet :one
INSERT INTO gym_exercise_sets (gym_visit_exercise_id,set_number,reps,weight)
VALUES ($1,$2,$3,$4) RETURNING id,set_number,reps,weight;
