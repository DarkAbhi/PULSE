BEGIN;

-- Some databases already ran a revision of migration 52 with this column.
-- Backfill legacy opt-ins only when adding it, so explicit opt-outs are preserved.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_attribute
        WHERE attrelid = 'user_quest_settings'::regclass AND attname = 'daily_meals' AND NOT attisdropped
    ) THEN
        ALTER TABLE user_quest_settings ADD COLUMN daily_meals BOOLEAN NOT NULL DEFAULT false;
        UPDATE user_quest_settings SET daily_meals = true WHERE meal_target IS NOT NULL;
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION quest_open_day(owner_id BIGINT, quest_date DATE, zone TEXT) RETURNS VOID LANGUAGE plpgsql AS $$
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
        ('sleep_goal', settings.sleep_minutes * 60)
    ) targets(id, target)
    JOIN quest_definitions d USING (id)
    WHERE targets.target IS NOT NULL;

    PERFORM quest_evaluate_day(owner_id, quest_date);
END;
$$;

CREATE OR REPLACE FUNCTION quest_evaluate_day(owner_id BIGINT, quest_date DATE) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    evaluated_at TIMESTAMPTZ := clock_timestamp();
    planned INTEGER := 0;
    eaten INTEGER;
BEGIN
    IF quest_date <> (evaluated_at AT TIME ZONE quest_user_timezone(owner_id))::date
       OR NOT EXISTS (
           SELECT 1 FROM quest_days WHERE user_id = owner_id AND day = quest_date
             AND quest_date = (evaluated_at AT TIME ZONE timezone)::date
       ) THEN
        RETURN;
    END IF;

    IF (
        SELECT daily_meals FROM user_quest_settings
        WHERE user_id = owner_id AND effective_date <= quest_date
        ORDER BY effective_date DESC LIMIT 1
    ) THEN
        SELECT count(*)::integer, count(*) FILTER (WHERE is_consumed)::integer
        INTO planned, eaten
        FROM meal_plans WHERE user_id = owner_id AND date = quest_date;
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

DROP TRIGGER quest_meals ON meal_plans;
DROP FUNCTION IF EXISTS quest_capture_meal_plan();

CREATE TRIGGER quest_meals AFTER INSERT OR UPDATE OF date, is_consumed OR DELETE ON meal_plans
FOR EACH ROW EXECUTE FUNCTION quest_capture_progress();

COMMIT;
