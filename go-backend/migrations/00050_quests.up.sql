BEGIN;

CREATE TABLE quest_definitions (
    id TEXT PRIMARY KEY,
    display_order SMALLINT NOT NULL UNIQUE,
    xp SMALLINT NOT NULL CHECK (xp > 0),
    hearts SMALLINT NOT NULL CHECK (hearts > 0)
);

INSERT INTO quest_definitions (id, display_order, xp, hearts) VALUES
    ('gym_workout', 1, 30, 2),
    ('daily_steps', 2, 20, 1),
    ('activity_rings', 3, 30, 2),
    ('sleep_goal', 4, 25, 2),
    ('daily_meals', 5, 20, 1);

-- Each revision takes effect on a local calendar date. NULL targets disable their quests.
CREATE TABLE user_quest_settings (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    effective_date DATE NOT NULL,
    gym_workout BOOLEAN NOT NULL,
    step_target INTEGER CHECK (step_target BETWEEN 1 AND 100000),
    activity_rings BOOLEAN NOT NULL,
    sleep_minutes INTEGER CHECK (sleep_minutes BETWEEN 1 AND 1440),
    meal_target INTEGER CHECK (meal_target BETWEEN 1 AND 24),
    PRIMARY KEY (user_id, effective_date)
);

-- Keep dates without scheduled quests so they can be skipped in a streak.
CREATE TABLE quest_days (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    day DATE NOT NULL,
    timezone TEXT NOT NULL,
    PRIMARY KEY (user_id, day)
);

CREATE TABLE daily_quests (
    user_id BIGINT NOT NULL,
    day DATE NOT NULL,
    quest_id TEXT NOT NULL REFERENCES quest_definitions(id),
    target INTEGER NOT NULL CHECK (target > 0),
    progress INTEGER NOT NULL DEFAULT 0 CHECK (progress >= 0 AND progress <= target),
    xp SMALLINT NOT NULL CHECK (xp > 0),
    hearts SMALLINT NOT NULL CHECK (hearts > 0),
    PRIMARY KEY (user_id, day, quest_id),
    FOREIGN KEY (user_id, day) REFERENCES quest_days(user_id, day) ON DELETE CASCADE
);

CREATE TABLE quest_reward_ledger (
    user_id BIGINT NOT NULL,
    day DATE NOT NULL,
    quest_id TEXT NOT NULL,
    xp SMALLINT NOT NULL CHECK (xp > 0),
    hearts SMALLINT NOT NULL CHECK (hearts > 0),
    earned_at TIMESTAMPTZ NOT NULL DEFAULT statement_timestamp(),
    PRIMARY KEY (user_id, day, quest_id),
    FOREIGN KEY (user_id, day, quest_id) REFERENCES daily_quests(user_id, day, quest_id) ON DELETE CASCADE
);

CREATE FUNCTION quest_user_timezone(owner_id BIGINT) RETURNS TEXT LANGUAGE sql STABLE AS $$
    SELECT COALESCE((SELECT timezone FROM user_profiles WHERE user_id = owner_id), 'UTC');
$$;

-- Source writes from both servers and quest requests share one transaction lock per user.
CREATE FUNCTION quest_lock(owner_id BIGINT) RETURNS VOID LANGUAGE sql AS $$
    SELECT pg_advisory_xact_lock(hashtextextended('pulse:quests:' || owner_id::text, 0));
$$;

CREATE FUNCTION quest_evaluate_day(owner_id BIGINT, quest_date DATE) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    zone TEXT;
BEGIN
    SELECT timezone INTO zone FROM quest_days WHERE user_id = owner_id AND day = quest_date;
    IF NOT FOUND THEN
        RETURN;
    END IF;

    WITH source_progress AS (
        SELECT (EXISTS (
            SELECT 1 FROM gym_visits AS visit
            JOIN gym_visit_exercises AS exercise ON exercise.gym_visit_id = visit.id
            JOIN gym_exercise_sets AS exercise_set ON exercise_set.gym_visit_exercise_id = exercise.id
            WHERE visit.user_id = owner_id AND exercise_set.reps > 0
              AND visit.created_at >= (quest_date::timestamp AT TIME ZONE zone)
              AND visit.created_at < ((quest_date + 1)::timestamp AT TIME ZONE zone)
        ) OR EXISTS (
            SELECT 1 FROM fitness_workouts AS workout
            JOIN workout_activity_types AS activity ON activity.id = workout.activity_type_id
            WHERE workout.user_id = owner_id AND activity.raw_value = 50 AND workout.duration_seconds > 0
              AND workout.start_time >= (quest_date::timestamp AT TIME ZONE zone)
              AND workout.start_time < ((quest_date + 1)::timestamp AT TIME ZONE zone)
        ))::integer AS workouts,
        COALESCE(rings.steps_count, 0) AS steps,
        (COALESCE(rings.move_calories_goal > 0 AND rings.move_calories >= rings.move_calories_goal, false)::integer
         + COALESCE(rings.exercise_minutes_goal > 0 AND rings.exercise_minutes >= rings.exercise_minutes_goal, false)::integer
         + COALESCE(rings.stand_hours_goal > 0 AND rings.stand_hours >= rings.stand_hours_goal, false)::integer) AS rings_closed,
        COALESCE(sleep.total_asleep_seconds, 0) AS sleep_seconds,
        (SELECT count(*)::integer FROM meal_plans
            WHERE user_id = owner_id AND date = quest_date AND is_consumed) AS meals
        FROM (SELECT 1) base
        LEFT JOIN fitness_activity_rings rings ON rings.user_id = owner_id AND rings.summary_date = quest_date
        LEFT JOIN fitness_sleep sleep ON sleep.user_id = owner_id AND sleep.sleep_date = quest_date
    ), measured AS (
        SELECT d.quest_id, LEAST(d.target, CASE d.quest_id
            WHEN 'gym_workout' THEN source_progress.workouts
            WHEN 'daily_steps' THEN source_progress.steps
            WHEN 'activity_rings' THEN source_progress.rings_closed
            WHEN 'sleep_goal' THEN source_progress.sleep_seconds
            WHEN 'daily_meals' THEN source_progress.meals
        END) AS progress
        FROM daily_quests d CROSS JOIN source_progress
        WHERE d.user_id = owner_id AND d.day = quest_date
    )
    UPDATE daily_quests d SET progress = m.progress
    FROM measured m
    WHERE d.user_id = owner_id AND d.day = quest_date AND d.quest_id = m.quest_id
      AND m.progress <> d.progress AND d.progress < d.target;

    INSERT INTO quest_reward_ledger (user_id, day, quest_id, xp, hearts)
    SELECT user_id, day, quest_id, xp, hearts FROM daily_quests
    WHERE user_id = owner_id AND day = quest_date AND progress = target
    ON CONFLICT DO NOTHING;
END;
$$;

CREATE FUNCTION quest_open_day(owner_id BIGINT, quest_date DATE, zone TEXT) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    settings user_quest_settings%ROWTYPE;
BEGIN
    IF EXISTS (SELECT 1 FROM quest_days WHERE user_id = owner_id AND day = quest_date) THEN
        RETURN;
    END IF;
    SELECT * INTO settings FROM user_quest_settings
    WHERE user_id = owner_id AND effective_date <= quest_date
    ORDER BY effective_date DESC LIMIT 1;
    IF NOT FOUND THEN
        RETURN;
    END IF;

    INSERT INTO quest_days (user_id, day, timezone) VALUES (owner_id, quest_date, zone);
    INSERT INTO daily_quests (user_id, day, quest_id, target, xp, hearts)
    SELECT owner_id, quest_date, d.id, targets.target, d.xp, d.hearts
    FROM (VALUES
        ('gym_workout', CASE WHEN settings.gym_workout AND EXISTS (
            SELECT 1 FROM fitness_workout_schedule
            WHERE user_id = owner_id AND weekday = extract(isodow FROM quest_date) AND workout_type <> 'rest_day'
        ) THEN 1 END),
        ('daily_steps', settings.step_target),
        ('activity_rings', CASE WHEN settings.activity_rings THEN 3 END),
        ('sleep_goal', settings.sleep_minutes * 60),
        ('daily_meals', settings.meal_target)
    ) targets(id, target)
    JOIN quest_definitions d USING (id)
    WHERE targets.target IS NOT NULL;

    PERFORM quest_evaluate_day(owner_id, quest_date);
END;
$$;

CREATE FUNCTION quest_open_through(owner_id BIGINT, local_today DATE, zone TEXT) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    unopened DATE;
BEGIN
    FOR unopened IN
        SELECT first_day + day_offset
        FROM (SELECT min(effective_date) AS first_day FROM user_quest_settings WHERE user_id = owner_id) settings
        CROSS JOIN LATERAL generate_series(0, local_today - first_day) AS offsets(day_offset)
        WHERE NOT EXISTS (
            SELECT 1 FROM quest_days WHERE user_id = owner_id AND day = first_day + day_offset
        )
        ORDER BY day_offset
    LOOP
        PERFORM quest_open_day(owner_id, unopened, zone);
    END LOOP;
END;
$$;

CREATE FUNCTION quest_capture_progress() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    owner_id BIGINT;
    zone TEXT;
    local_today DATE;
    visit_time TIMESTAMPTZ;
    old_date DATE;
    new_date DATE;
    affected DATE;
BEGIN
    IF TG_TABLE_NAME = 'gym_exercise_sets' THEN
        SELECT visit.user_id, visit.created_at INTO owner_id, visit_time
        FROM gym_visit_exercises AS exercise
        JOIN gym_visits AS visit ON visit.id = exercise.gym_visit_id
        WHERE exercise.id = NEW.gym_visit_exercise_id;
    ELSIF TG_OP = 'DELETE' THEN
        owner_id := OLD.user_id;
    ELSE
        owner_id := NEW.user_id;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM users WHERE id = owner_id) THEN
        RETURN NULL;
    END IF;
    PERFORM quest_lock(owner_id);
    IF NOT EXISTS (SELECT 1 FROM user_quest_settings WHERE user_id = owner_id) THEN
        RETURN NULL;
    END IF;

    zone := quest_user_timezone(owner_id);
    local_today := (clock_timestamp() AT TIME ZONE zone)::date;
    CASE TG_TABLE_NAME
        WHEN 'gym_visits' THEN
            IF TG_OP <> 'INSERT' THEN old_date := (OLD.created_at AT TIME ZONE zone)::date; END IF;
            IF TG_OP <> 'DELETE' THEN new_date := (NEW.created_at AT TIME ZONE zone)::date; END IF;
        WHEN 'gym_exercise_sets' THEN
            new_date := (visit_time AT TIME ZONE zone)::date;
        WHEN 'fitness_workouts' THEN
            new_date := (NEW.start_time AT TIME ZONE zone)::date;
        WHEN 'meal_plans' THEN
            IF TG_OP <> 'INSERT' THEN old_date := OLD.date; END IF;
            IF TG_OP <> 'DELETE' THEN new_date := NEW.date; END IF;
        WHEN 'fitness_activity_rings' THEN
            IF TG_OP <> 'INSERT' THEN old_date := OLD.summary_date; END IF;
            IF TG_OP <> 'DELETE' THEN new_date := NEW.summary_date; END IF;
        WHEN 'fitness_sleep' THEN
            IF TG_OP <> 'INSERT' THEN old_date := OLD.sleep_date; END IF;
            IF TG_OP <> 'DELETE' THEN new_date := NEW.sleep_date; END IF;
    END CASE;

    FOR affected IN
        SELECT DISTINCT source_day FROM unnest(ARRAY[old_date, new_date]) AS dates(source_day)
        WHERE source_day <= local_today
    LOOP
        IF EXISTS (SELECT 1 FROM quest_days WHERE user_id = owner_id AND day = affected) THEN
            PERFORM quest_evaluate_day(owner_id, affected);
        ELSE
            PERFORM quest_open_day(owner_id, affected, zone);
        END IF;
    END LOOP;
    RETURN NULL;
END;
$$;

-- Preserve assigned workout days before editing the live workout schedule.
CREATE FUNCTION quest_snapshot_workout_schedule() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    owner_id BIGINT := CASE WHEN TG_OP = 'DELETE' THEN OLD.user_id ELSE NEW.user_id END;
    zone TEXT;
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE id = owner_id) THEN
        PERFORM quest_lock(owner_id);
        IF EXISTS (SELECT 1 FROM user_quest_settings WHERE user_id = owner_id) THEN
            zone := quest_user_timezone(owner_id);
            PERFORM quest_open_through(owner_id, (clock_timestamp() AT TIME ZONE zone)::date, zone);
        END IF;
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER quest_workout_schedule BEFORE INSERT OR UPDATE OR DELETE ON fitness_workout_schedule
FOR EACH ROW EXECUTE FUNCTION quest_snapshot_workout_schedule();

CREATE TRIGGER quest_workouts AFTER INSERT OR UPDATE OF created_at OR DELETE ON gym_visits
FOR EACH ROW EXECUTE FUNCTION quest_capture_progress();

CREATE TRIGGER quest_workout_sets AFTER INSERT OR UPDATE OF reps ON gym_exercise_sets
FOR EACH ROW WHEN (NEW.reps > 0) EXECUTE FUNCTION quest_capture_progress();

CREATE TRIGGER quest_synced_workouts AFTER INSERT OR UPDATE OF activity_type_id, duration_seconds, start_time ON fitness_workouts
FOR EACH ROW WHEN (NEW.duration_seconds > 0) EXECUTE FUNCTION quest_capture_progress();

CREATE TRIGGER quest_meals AFTER INSERT OR UPDATE OF date, is_consumed OR DELETE ON meal_plans
FOR EACH ROW EXECUTE FUNCTION quest_capture_progress();

CREATE TRIGGER quest_rings AFTER INSERT OR UPDATE OR DELETE ON fitness_activity_rings
FOR EACH ROW EXECUTE FUNCTION quest_capture_progress();

CREATE TRIGGER quest_sleep AFTER INSERT OR UPDATE OR DELETE ON fitness_sleep
FOR EACH ROW EXECUTE FUNCTION quest_capture_progress();

COMMIT;
