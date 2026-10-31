BEGIN;

ALTER TABLE gym_visits
    ADD COLUMN fitness_workout_id BIGINT UNIQUE REFERENCES fitness_workouts(id) ON DELETE SET NULL;

INSERT INTO gym_visits (user_id, fitness_workout_id, created_at, updated_at)
SELECT w.user_id, w.id, w.start_time, w.start_time
FROM (
    SELECT DISTINCT ON (user_id, (start_time AT TIME ZONE 'Asia/Kolkata')::date)
        id, user_id, start_time
    FROM fitness_workouts
    WHERE activity_type_id = 3
    ORDER BY user_id, (start_time AT TIME ZONE 'Asia/Kolkata')::date, start_time, id
) AS w
WHERE NOT EXISTS (
    SELECT 1
    FROM gym_visits AS g
    WHERE g.user_id = w.user_id
      AND (g.created_at AT TIME ZONE 'Asia/Kolkata')::date =
          (w.start_time AT TIME ZONE 'Asia/Kolkata')::date
);

COMMIT;
