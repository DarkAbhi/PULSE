BEGIN;

-- Retain old manual targets for rollback. New quest days use the meal plan.
CREATE OR REPLACE FUNCTION quest_open_day(owner_id BIGINT, quest_date DATE, zone TEXT) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    settings user_quest_settings%ROWTYPE;
BEGIN
    SELECT * INTO settings FROM user_quest_settings
    WHERE user_id = owner_id AND effective_date <= quest_date
    ORDER BY effective_date DESC LIMIT 1;
    IF NOT FOUND THEN
        PERFORM quest_evaluate_day(owner_id, quest_date);
        RETURN;
    END IF;

    INSERT INTO quest_days (user_id, day, timezone) VALUES (owner_id, quest_date, zone)
    ON CONFLICT DO NOTHING;
    INSERT INTO daily_quests (user_id, day, quest_id, target, xp, hearts)
    SELECT owner_id, quest_date, d.id, targets.target, d.xp, d.hearts
    FROM (VALUES
        ('gym_workout', CASE WHEN settings.gym_workout AND EXISTS (
            SELECT 1 FROM fitness_workout_schedule
            WHERE user_id = owner_id AND weekday = extract(isodow FROM quest_date) AND workout_type <> 'rest_day'
        ) THEN 1 END),
        ('daily_steps', settings.step_target),
        ('activity_rings', CASE WHEN settings.activity_rings THEN 3 END),
        ('sleep_goal', settings.sleep_minutes * 60)
    ) targets(id, target)
    JOIN quest_definitions d USING (id)
    WHERE targets.target IS NOT NULL
    ON CONFLICT DO NOTHING;

    PERFORM quest_evaluate_day(owner_id, quest_date);
END;
$$;

CREATE OR REPLACE FUNCTION quest_evaluate_day(owner_id BIGINT, quest_date DATE) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    evaluated_at TIMESTAMPTZ := clock_timestamp();
    zone TEXT := quest_user_timezone(owner_id);
    planned INTEGER;
    eaten INTEGER;
BEGIN
    IF quest_date <> (evaluated_at AT TIME ZONE zone)::date THEN
        RETURN;
    END IF;

    SELECT count(*)::integer, count(*) FILTER (WHERE is_consumed)::integer
    INTO planned, eaten
    FROM meal_plans WHERE user_id = owner_id AND date = quest_date;

    IF planned > 0 THEN
        INSERT INTO quest_days (user_id, day, timezone) VALUES (owner_id, quest_date, zone)
        ON CONFLICT DO NOTHING;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM quest_days WHERE user_id = owner_id AND day = quest_date
          AND quest_date = (evaluated_at AT TIME ZONE timezone)::date
    ) THEN
        RETURN;
    END IF;

    IF planned = 0 THEN
        DELETE FROM daily_quests WHERE user_id = owner_id AND day = quest_date AND quest_id = 'daily_meals';
    ELSE
        INSERT INTO daily_quests (user_id, day, quest_id, target, progress, xp, hearts)
        SELECT owner_id, quest_date, id, planned, eaten, xp, hearts
        FROM quest_definitions WHERE id = 'daily_meals'
        ON CONFLICT (user_id, day, quest_id) DO UPDATE
        SET target = EXCLUDED.target, progress = EXCLUDED.progress
        WHERE (daily_quests.target, daily_quests.progress) IS DISTINCT FROM (EXCLUDED.target, EXCLUDED.progress);

        -- Plan edits and uneating a meal can reopen today's quest.
        IF eaten < planned THEN
            DELETE FROM quest_reward_ledger
            WHERE user_id = owner_id AND day = quest_date AND quest_id = 'daily_meals';
        END IF;
    END IF;

    PERFORM quest_update_progress(owner_id, quest_date);
END;
$$;

CREATE FUNCTION quest_capture_meal_plan() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    owner_id BIGINT := CASE WHEN TG_OP = 'DELETE' THEN OLD.user_id ELSE NEW.user_id END;
    zone TEXT;
    local_today DATE;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE id = owner_id) THEN
        RETURN NULL;
    END IF;

    PERFORM quest_lock(owner_id);
    zone := quest_user_timezone(owner_id);
    local_today := (clock_timestamp() AT TIME ZONE zone)::date;
    IF (TG_OP <> 'INSERT' AND OLD.date = local_today) OR (TG_OP <> 'DELETE' AND NEW.date = local_today) THEN
        IF NOT EXISTS (SELECT 1 FROM quest_days WHERE user_id = owner_id AND day = local_today) THEN
            PERFORM quest_open_day(owner_id, local_today, zone);
            RETURN NULL;
        END IF;
        PERFORM quest_evaluate_day(owner_id, local_today);
    END IF;
    RETURN NULL;
END;
$$;

DROP TRIGGER quest_meals ON meal_plans;

CREATE TRIGGER quest_meals AFTER INSERT OR UPDATE OF date, is_consumed OR DELETE ON meal_plans
FOR EACH ROW EXECUTE FUNCTION quest_capture_meal_plan();

COMMIT;
