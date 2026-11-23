ALTER TABLE gym_visits
    ADD COLUMN workout_type TEXT,
    ADD COLUMN workout_focus VARCHAR(120),
    ADD CONSTRAINT gym_visits_workout_type_check CHECK (
        workout_type IN ('single_muscle', 'double_muscle', 'push', 'pull', 'legs',
                         'upper_body', 'lower_body', 'full_body', 'cardio', 'mobility', 'other')
    ),
    ADD CONSTRAINT gym_visits_workout_focus_check CHECK (workout_focus IS NULL OR workout_type IS NOT NULL);

CREATE TABLE fitness_workout_schedule (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    weekday SMALLINT NOT NULL CHECK (weekday BETWEEN 1 AND 7),
    workout_type TEXT NOT NULL CHECK (
        workout_type IN ('single_muscle', 'double_muscle', 'push', 'pull', 'legs',
                         'upper_body', 'lower_body', 'full_body', 'cardio', 'mobility', 'other', 'rest_day')
    ),
    workout_focus VARCHAR(120),
    PRIMARY KEY (user_id, weekday),
    CHECK (workout_type <> 'rest_day' OR workout_focus IS NULL)
);
