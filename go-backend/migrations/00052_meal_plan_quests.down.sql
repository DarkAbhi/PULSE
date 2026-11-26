BEGIN;

DROP TRIGGER quest_meals ON meal_plans;
DROP FUNCTION quest_capture_meal_plan();

CREATE TRIGGER quest_meals AFTER INSERT OR UPDATE OF date, is_consumed OR DELETE ON meal_plans
FOR EACH ROW EXECUTE FUNCTION quest_capture_progress();

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
        ('sleep_goal', settings.sleep_minutes * 60),
        ('daily_meals', settings.meal_target)
    ) targets(id, target)
    JOIN quest_definitions d USING (id)
    WHERE targets.target IS NOT NULL;

    PERFORM quest_evaluate_day(owner_id, quest_date);
END;
$$;

CREATE OR REPLACE FUNCTION quest_evaluate_day(owner_id BIGINT, quest_date DATE) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    evaluated_at TIMESTAMPTZ := clock_timestamp();
BEGIN
    IF quest_date = (evaluated_at AT TIME ZONE quest_user_timezone(owner_id))::date
       AND EXISTS (
           SELECT 1 FROM quest_days
           WHERE user_id = owner_id AND day = quest_date
             AND quest_date = (evaluated_at AT TIME ZONE timezone)::date
       ) THEN
        PERFORM quest_update_progress(owner_id, quest_date);
    END IF;
END;
$$;

COMMIT;
