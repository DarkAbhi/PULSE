DROP TABLE fitness_workout_schedule;

ALTER TABLE gym_visits
    DROP COLUMN workout_focus,
    DROP COLUMN workout_type;
