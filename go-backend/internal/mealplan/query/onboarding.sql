-- name: LockMealPreferenceUser :one
SELECT id FROM users WHERE id = $1 FOR UPDATE;

-- name: GetMealOnboarding :one
SELECT status, questionnaire_version, decided_at FROM meal_plan_onboarding WHERE user_id = $1;

-- name: GetMealPreferences :one
SELECT goal, dietary_pattern, barriers, food_avoidances FROM meal_preferences WHERE user_id = $1;

-- name: ListMealPreferenceTimes :many
SELECT meal_kind, to_char(usual_time, 'HH24:MI')::TEXT AS usual_time
FROM meal_preference_times WHERE user_id = $1
ORDER BY CASE meal_kind WHEN 'breakfast' THEN 1 WHEN 'lunch' THEN 2 ELSE 3 END;

-- name: CreateMealOnboarding :exec
INSERT INTO meal_plan_onboarding (user_id, status, questionnaire_version) VALUES ($1, $2, $3);

-- name: UpsertMealPreferences :exec
INSERT INTO meal_preferences (user_id, goal, dietary_pattern, barriers, food_avoidances)
VALUES ($1, $2, $3, $4, $5)
ON CONFLICT (user_id) DO UPDATE SET goal = EXCLUDED.goal,
dietary_pattern = EXCLUDED.dietary_pattern, barriers = EXCLUDED.barriers,
food_avoidances = EXCLUDED.food_avoidances, updated_at = CURRENT_TIMESTAMP;

-- name: DeleteMealPreferenceTimes :exec
DELETE FROM meal_preference_times WHERE user_id = $1;

-- name: CreateMealPreferenceTime :exec
INSERT INTO meal_preference_times (user_id, meal_kind, usual_time)
VALUES ($1, $2, sqlc.arg(usual_time)::TEXT::TIME);
