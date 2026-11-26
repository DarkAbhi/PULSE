BEGIN;

DROP TRIGGER quest_sleep ON fitness_sleep;
DROP TRIGGER quest_rings ON fitness_activity_rings;
DROP TRIGGER quest_meals ON meal_plans;
DROP TRIGGER quest_synced_workouts ON fitness_workouts;
DROP TRIGGER quest_workout_sets ON gym_exercise_sets;
DROP TRIGGER quest_workouts ON gym_visits;
DROP TRIGGER quest_workout_schedule ON fitness_workout_schedule;
DROP FUNCTION quest_snapshot_workout_schedule();
DROP FUNCTION quest_capture_progress();
DROP FUNCTION quest_open_through(BIGINT, DATE, TEXT);
DROP FUNCTION quest_open_day(BIGINT, DATE, TEXT);
DROP FUNCTION quest_evaluate_day(BIGINT, DATE);
DROP FUNCTION quest_lock(BIGINT);
DROP FUNCTION quest_user_timezone(BIGINT);
DROP TABLE quest_reward_ledger, daily_quests, quest_days, user_quest_settings, quest_definitions;

COMMIT;
