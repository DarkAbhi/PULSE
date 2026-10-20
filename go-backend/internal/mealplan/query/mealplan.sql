-- name: CreateMealPlan :one
WITH inserted AS (
    INSERT INTO meal_plans (user_id, date, name, meal_time_id, start_time, end_time, is_consumed)
    VALUES ($1, ($2::text)::date, $3, $4, NULLIF($5::text, '')::time, NULLIF($6::text, '')::time, $7)
    RETURNING id, date, name, meal_time_id, start_time, end_time, is_consumed, created_at
)
SELECT i.id, TO_CHAR(i.date, 'YYYY-MM-DD') AS date, i.name, i.meal_time_id, mt.name AS meal_time_name,
       (COALESCE(TO_CHAR(COALESCE(i.start_time, mt.start_time), 'HH24:MI'), ''))::text AS start_time,
       (COALESCE(TO_CHAR(COALESCE(i.end_time, mt.end_time), 'HH24:MI'), ''))::text AS end_time,
       i.is_consumed, i.created_at
FROM inserted i LEFT JOIN meal_times mt ON i.meal_time_id = mt.id;

-- name: CreateMealTime :one
INSERT INTO meal_times (user_id, name, start_time, end_time, is_default)
VALUES ($1, $2, ($3::text)::time, ($4::text)::time, false)
RETURNING id, name, TO_CHAR(start_time, 'HH24:MI') AS start_time, TO_CHAR(end_time, 'HH24:MI') AS end_time, is_default, user_id;

-- name: DeleteMealPlan :execrows
DELETE FROM meal_plans WHERE id = $1 AND user_id = $2;

-- name: DeleteMealTime :execrows
DELETE FROM meal_times WHERE id = $1 AND user_id = $2 AND is_default = false;

-- name: EnsureDefaultMealTime :exec
INSERT INTO meal_times (name, start_time, end_time, is_default)
VALUES ($1, ($2::text)::time, ($3::text)::time, true)
ON CONFLICT (COALESCE(user_id, 0), LOWER(name)) DO NOTHING;

-- name: GetMealPlanForUpdate :one
SELECT name, TO_CHAR(date, 'YYYY-MM-DD') AS date, meal_time_id, is_consumed
FROM meal_plans WHERE id = $1 AND user_id = $2;

-- name: GetMealTimeRange :one
SELECT TO_CHAR(start_time, 'HH24:MI:SS') AS start_time, TO_CHAR(end_time, 'HH24:MI:SS') AS end_time
FROM meal_times WHERE id = $1 AND (user_id IS NULL OR user_id = $2);

-- name: ListMealPlansByDate :many
SELECT mp.id, TO_CHAR(mp.date, 'YYYY-MM-DD') AS date, mp.name, mp.meal_time_id, mt.name AS meal_time_name,
       (COALESCE(TO_CHAR(COALESCE(mp.start_time, mt.start_time), 'HH24:MI'), ''))::text AS start_time,
       (COALESCE(TO_CHAR(COALESCE(mp.end_time, mt.end_time), 'HH24:MI'), ''))::text AS end_time,
       mp.is_consumed, mp.created_at
FROM meal_plans mp LEFT JOIN meal_times mt ON mp.meal_time_id = mt.id
WHERE mp.user_id = $1 AND mp.date = ($2::text)::date
ORDER BY COALESCE(mp.start_time, mt.start_time) ASC NULLS LAST, mp.created_at ASC, mp.id ASC;

-- name: ListMealPlansByRange :many
SELECT mp.id, TO_CHAR(mp.date, 'YYYY-MM-DD') AS date, mp.name, mp.meal_time_id, mt.name AS meal_time_name,
       (COALESCE(TO_CHAR(COALESCE(mp.start_time, mt.start_time), 'HH24:MI'), ''))::text AS start_time,
       (COALESCE(TO_CHAR(COALESCE(mp.end_time, mt.end_time), 'HH24:MI'), ''))::text AS end_time,
       mp.is_consumed, mp.created_at
FROM meal_plans mp LEFT JOIN meal_times mt ON mp.meal_time_id = mt.id
WHERE mp.user_id = $1 AND mp.date >= ($2::text)::date AND mp.date <= ($3::text)::date
ORDER BY mp.date ASC, COALESCE(mp.start_time, mt.start_time) ASC NULLS LAST, mp.created_at ASC, mp.id ASC;

-- name: ListMealTimes :many
SELECT id, name, TO_CHAR(start_time, 'HH24:MI') AS start_time, TO_CHAR(end_time, 'HH24:MI') AS end_time, is_default, user_id
FROM meal_times WHERE user_id IS NULL OR user_id = $1
ORDER BY start_time ASC, id ASC;

-- name: ListRecentMealPlans :many
SELECT mp.id, TO_CHAR(mp.date, 'YYYY-MM-DD') AS date, mp.name, mp.meal_time_id, mt.name AS meal_time_name,
       (COALESCE(TO_CHAR(COALESCE(mp.start_time, mt.start_time), 'HH24:MI'), ''))::text AS start_time,
       (COALESCE(TO_CHAR(COALESCE(mp.end_time, mt.end_time), 'HH24:MI'), ''))::text AS end_time,
       mp.is_consumed, mp.created_at
FROM meal_plans mp LEFT JOIN meal_times mt ON mp.meal_time_id = mt.id
WHERE mp.user_id = $1
ORDER BY mp.date DESC, COALESCE(mp.start_time, mt.start_time) ASC NULLS LAST, mp.created_at ASC, mp.id ASC
LIMIT 100;

-- name: SetMealPlanConsumed :one
WITH updated AS (
    UPDATE meal_plans SET is_consumed = $1, updated_at = CURRENT_TIMESTAMP
    WHERE meal_plans.id = $2 AND meal_plans.user_id = $3
    RETURNING id, date, name, meal_time_id, start_time, end_time, is_consumed, created_at
)
SELECT u.id, TO_CHAR(u.date, 'YYYY-MM-DD') AS date, u.name, u.meal_time_id, mt.name AS meal_time_name,
       (COALESCE(TO_CHAR(COALESCE(u.start_time, mt.start_time), 'HH24:MI'), ''))::text AS start_time,
       (COALESCE(TO_CHAR(COALESCE(u.end_time, mt.end_time), 'HH24:MI'), ''))::text AS end_time,
       u.is_consumed, u.created_at
FROM updated u LEFT JOIN meal_times mt ON u.meal_time_id = mt.id;

-- name: UpdateMealPlan :one
WITH updated AS (
    UPDATE meal_plans
    SET name = $1, date = ($2::text)::date, meal_time_id = $3,
        start_time = NULLIF($4::text, '')::time, end_time = NULLIF($5::text, '')::time,
        is_consumed = $6, updated_at = CURRENT_TIMESTAMP
    WHERE meal_plans.id = $7 AND meal_plans.user_id = $8
    RETURNING id, date, name, meal_time_id, start_time, end_time, is_consumed, created_at
)
SELECT u.id, TO_CHAR(u.date, 'YYYY-MM-DD') AS date, u.name, u.meal_time_id, mt.name AS meal_time_name,
       (COALESCE(TO_CHAR(COALESCE(u.start_time, mt.start_time), 'HH24:MI'), ''))::text AS start_time,
       (COALESCE(TO_CHAR(COALESCE(u.end_time, mt.end_time), 'HH24:MI'), ''))::text AS end_time,
       u.is_consumed, u.created_at
FROM updated u LEFT JOIN meal_times mt ON u.meal_time_id = mt.id;
