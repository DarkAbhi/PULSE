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

-- name: DeleteExercise :execrows
DELETE FROM gym_visit_exercises AS e
USING gym_visits AS v
WHERE e.id = sqlc.arg(exercise_id)
  AND e.gym_visit_id = sqlc.arg(visit_id)
  AND v.id = e.gym_visit_id
  AND v.user_id = sqlc.arg(user_id);

-- name: VisitExists :one
SELECT EXISTS(SELECT 1 FROM gym_visits WHERE id=$1 AND user_id=$2);

-- name: ListExercises :many
SELECT e.id, e.name, e.exercise_catalog_id, c.name AS catalog_name, c.equipment, c.data
FROM gym_visit_exercises e
LEFT JOIN exercise_catalog c ON c.id = e.exercise_catalog_id
WHERE e.gym_visit_id=$1 ORDER BY e.id ASC;

-- name: ListSets :many
SELECT id,set_number,reps,weight FROM gym_exercise_sets WHERE gym_visit_exercise_id=$1 ORDER BY set_number ASC;

-- name: CreateExercise :one
INSERT INTO gym_visit_exercises (gym_visit_id,name,exercise_catalog_id)
VALUES ($1,$2,$3) RETURNING id,name,exercise_catalog_id;

-- name: CreateSet :one
INSERT INTO gym_exercise_sets (gym_visit_exercise_id,set_number,reps,weight)
VALUES ($1,$2,$3,$4) RETURNING id,set_number,reps,weight;

-- name: SearchExerciseCatalog :many
SELECT c.id, c.name, c.equipment, c.data,
    CASE WHEN c.normalized_name = sqlc.arg(search)::text THEN 'exact'
         WHEN a.exercise_catalog_id IS NOT NULL THEN 'alias'
         ELSE 'suggested' END::text AS match_type
FROM exercise_catalog c
LEFT JOIN exercise_aliases a ON a.exercise_catalog_id = c.id
    AND a.user_id = sqlc.arg(user_id) AND a.normalized_alias = sqlc.arg(search)::text
WHERE c.normalized_name = sqlc.arg(search)::text
   OR a.exercise_catalog_id IS NOT NULL
   OR c.normalized_name % sqlc.arg(search)::text
   OR strpos(c.normalized_name, sqlc.arg(search)::text) > 0
ORDER BY CASE WHEN c.normalized_name = sqlc.arg(search)::text THEN 0
              WHEN a.exercise_catalog_id IS NOT NULL THEN 1 ELSE 2 END,
    similarity(c.normalized_name, sqlc.arg(search)::text) DESC, c.name, c.id
LIMIT 8;

-- name: GetCatalogExercise :one
SELECT id, name, equipment, data FROM exercise_catalog WHERE id=$1;

-- name: RememberExerciseAlias :exec
INSERT INTO exercise_aliases (user_id, normalized_alias, exercise_catalog_id)
VALUES ($1,$2,$3)
ON CONFLICT (user_id, normalized_alias)
DO UPDATE SET exercise_catalog_id = EXCLUDED.exercise_catalog_id;
