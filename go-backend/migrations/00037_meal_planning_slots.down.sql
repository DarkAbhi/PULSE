BEGIN;

DELETE FROM meal_preference_times WHERE meal_kind = 'evening_snacks';

-- Version 1 requires at least one usual meal per saved preference.
INSERT INTO meal_preference_times (user_id, meal_kind, usual_time)
SELECT user_id, 'breakfast', '09:00'::TIME FROM meal_preferences p
WHERE NOT EXISTS (SELECT 1 FROM meal_preference_times t WHERE t.user_id = p.user_id);

ALTER TABLE meal_preference_times
    DROP CONSTRAINT meal_preference_times_meal_kind_check,
    ADD CONSTRAINT meal_preference_times_meal_kind_check
        CHECK (meal_kind IN ('breakfast', 'lunch', 'dinner'));

ALTER TABLE meal_preferences DROP COLUMN planning_slots_configured;

COMMIT;
