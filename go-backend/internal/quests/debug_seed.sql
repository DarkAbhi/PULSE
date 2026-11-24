-- The caller prepares the unused past dates and holds the user's quest lock.
-- Open the snapshots first so source triggers leave historical progress frozen.
INSERT INTO quest_days (user_id, day, timezone)
SELECT user_id, day, zone FROM pg_temp.debug_quest_days;

-- The five bits describe which source records should meet their goals.
-- 1 = workout, 2 = steps, 4 = rings, 8 = sleep, 16 = meals.
INSERT INTO fitness_activity_rings (
    user_id, summary_date, steps_count, move_calories, move_calories_goal,
    exercise_minutes, exercise_minutes_goal, stand_hours, stand_hours_goal, created_at, updated_at
)
SELECT user_id, day,
    CASE WHEN goals & 2 <> 0 THEN 8250 + sample * 19 ELSE 2800 + sample * 19 END,
    CASE WHEN goals & 4 <> 0 THEN 450 + sample ELSE 200 + sample % 200 END, 450,
    CASE WHEN goals & 4 <> 0 THEN 32 + sample % 30 ELSE 10 + sample % 20 END, 30,
    CASE WHEN goals & 4 <> 0 THEN 12 + sample % 3 ELSE 6 + sample % 6 END, 12,
    (day + time '21:00') AT TIME ZONE zone, (day + time '21:00') AT TIME ZONE zone
FROM pg_temp.debug_quest_days;

INSERT INTO fitness_sleep (user_id, sleep_date, total_asleep_seconds, main_sleep_seconds, wake_at, sessions)
SELECT user_id, day, duration, duration, wake_at,
    jsonb_build_array(jsonb_build_object(
        'session_id', 'dev-sleep-' || day::text, 'start', wake_at - duration * interval '1 second',
        'end', wake_at, 'asleep_seconds', duration, 'is_main_sleep', true
    ))
FROM (
    SELECT user_id, day, (day + time '07:00') AT TIME ZONE zone AS wake_at,
        CASE WHEN goals & 8 <> 0 THEN 28800 + sample % 61 * 60 ELSE 21000 + sample % 120 * 60 END AS duration
    FROM pg_temp.debug_quest_days
) nights;

INSERT INTO meal_plans (user_id, date, name, meal_time_id, start_time, end_time, is_consumed, created_at, updated_at)
SELECT d.user_id, d.day,
    meal.dishes[1 + d.sample % cardinality(meal.dishes)], slot.id, meal.start_time, meal.end_time,
    d.goals & 16 <> 0 OR meal.position <= d.sample % 3,
    (d.day + meal.start_time) AT TIME ZONE d.zone, (d.day + meal.end_time) AT TIME ZONE d.zone
FROM pg_temp.debug_quest_days d
CROSS JOIN (VALUES
    (1, 'Breakfast', ARRAY['Oats with berries', 'Eggs and toast', 'Yogurt with fruit'], time '08:00', time '08:30'),
    (2, 'Lunch', ARRAY['Paneer rice bowl', 'Chickpea salad', 'Dal and rice'], time '13:00', time '13:30'),
    (3, 'Dinner', ARRAY['Tofu stir-fry', 'Lentil soup', 'Vegetable wraps'], time '19:30', time '20:00')
) meal(position, slot_name, dishes, start_time, end_time)
LEFT JOIN meal_times slot ON slot.user_id IS NULL AND slot.is_default AND slot.name = meal.slot_name;

WITH visits AS (
    INSERT INTO gym_visits (user_id, workout_type, created_at, updated_at)
    SELECT user_id, 'full_body', (day + time '17:00') AT TIME ZONE zone, (day + time '18:00') AT TIME ZONE zone
    FROM pg_temp.debug_quest_days WHERE goals & 1 <> 0
    RETURNING id, created_at
), exercises AS (
    INSERT INTO gym_visit_exercises (gym_visit_id, name, created_at, updated_at)
    SELECT v.id, exercise.name, v.created_at, v.created_at
    FROM visits v CROSS JOIN (VALUES ('Dumbbell Bench Press'), ('Lat Pulldown'), ('Goblet Squat')) exercise(name)
    RETURNING id, name, created_at
)
INSERT INTO gym_exercise_sets (gym_visit_exercise_id, set_number, reps, weight, created_at)
SELECT e.id, n, 12 - (n - 1) * 2,
    CASE e.name WHEN 'Dumbbell Bench Press' THEN 10 WHEN 'Lat Pulldown' THEN 25 ELSE 14 END
        + (n - 1) * 2.5,
    e.created_at + n * interval '5 minutes'
FROM exercises e CROSS JOIN generate_series(1, 3) sets(n);

INSERT INTO daily_quests (user_id, day, quest_id, target, xp, hearts)
SELECT d.user_id, d.day, definition.id, goals.target, definition.xp, definition.hearts
FROM pg_temp.debug_quest_days d
CROSS JOIN LATERAL (VALUES
    ('gym_workout', CASE WHEN d.goals & 1 <> 0 OR extract(isodow FROM d.day) IN (1, 3, 5) THEN 1 END),
    ('daily_steps', 8000), ('activity_rings', 3), ('sleep_goal', 28800),
    ('daily_meals', (SELECT count(*)::integer FROM meal_plans m WHERE m.user_id = d.user_id AND m.date = d.day))
) goals(id, target)
JOIN quest_definitions definition ON definition.id = goals.id
WHERE goals.target > 0;

-- Measure the saved records using the normal quest calculation. The wrapper
-- used by live writes deliberately ignores dates that have already closed.
SELECT quest_measure_progress(user_id, day) FROM pg_temp.debug_quest_days;

UPDATE quest_reward_ledger reward
SET earned_at = (d.day + time '21:00') AT TIME ZONE d.zone
FROM pg_temp.debug_quest_days d
WHERE reward.user_id = d.user_id AND reward.day = d.day;

-- The source inserts generate feed entries. Date those entries with the
-- activity itself so the entire seed doesn't appear in today's feed.
WITH source_events AS (
    SELECT a.id, m.created_at AS recorded_at
    FROM pg_temp.debug_quest_days d
    JOIN meal_plans m ON (m.user_id, m.date) = (d.user_id, d.day)
    JOIN recent_activity a ON a.user_id = d.user_id AND a.meal_id = m.id
    UNION ALL
    SELECT a.id, g.created_at
    FROM pg_temp.debug_quest_days d
    JOIN gym_visits g ON g.user_id = d.user_id AND g.created_at >= d.start_at AND g.created_at < d.end_at
    JOIN recent_activity a ON a.user_id = d.user_id AND a.gym_visit_id = g.id
    UNION ALL
    SELECT a.id, e.created_at
    FROM pg_temp.debug_quest_days d
    JOIN gym_visits g ON g.user_id = d.user_id AND g.created_at >= d.start_at AND g.created_at < d.end_at
    JOIN gym_visit_exercises e ON e.gym_visit_id = g.id
    JOIN recent_activity a ON a.user_id = d.user_id AND a.exercise_id = e.id
    UNION ALL
    SELECT a.id, s.wake_at
    FROM pg_temp.debug_quest_days d
    JOIN fitness_sleep s ON (s.user_id, s.sleep_date) = (d.user_id, d.day)
    JOIN recent_activity a ON (a.user_id, a.sleep_date) = (d.user_id, d.day)
    UNION ALL
    SELECT a.id, r.created_at
    FROM pg_temp.debug_quest_days d
    JOIN fitness_activity_rings r ON (r.user_id, r.summary_date) = (d.user_id, d.day)
    JOIN recent_activity a ON (a.user_id, a.rings_date) = (d.user_id, d.day)
)
UPDATE recent_activity a SET recorded_at = source_events.recorded_at
FROM source_events WHERE a.id = source_events.id;
