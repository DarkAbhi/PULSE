BEGIN;

ALTER TABLE meal_preferences
    ADD COLUMN planning_slots_configured BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE meal_preference_times
    DROP CONSTRAINT meal_preference_times_meal_kind_check,
    ADD CONSTRAINT meal_preference_times_meal_kind_check
        CHECK (meal_kind IN ('breakfast', 'lunch', 'evening_snacks', 'dinner'));

COMMIT;
