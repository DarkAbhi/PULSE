BEGIN;

ALTER FUNCTION quest_update_progress(BIGINT, DATE) RENAME TO quest_measure_progress;

-- quest_evaluate_day calls this only for the open local day.
CREATE FUNCTION quest_update_progress(owner_id BIGINT, quest_date DATE) RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    IF (
        SELECT gym_workout FROM user_quest_settings
        WHERE user_id = owner_id AND effective_date <= quest_date
        ORDER BY effective_date DESC LIMIT 1
    ) AND EXISTS (
        SELECT 1 FROM fitness_workout_schedule
        WHERE user_id = owner_id AND weekday = extract(isodow FROM quest_date) AND workout_type <> 'rest_day'
    ) THEN
        INSERT INTO daily_quests (user_id, day, quest_id, target, xp, hearts)
        SELECT owner_id, quest_date, id, 1, xp, hearts
        FROM quest_definitions WHERE id = 'gym_workout'
        ON CONFLICT (user_id, day, quest_id) DO NOTHING;
    ELSE
        -- The foreign key also removes any XP and hearts earned for this quest.
        DELETE FROM daily_quests WHERE user_id = owner_id AND day = quest_date AND quest_id = 'gym_workout';
    END IF;

    PERFORM quest_measure_progress(owner_id, quest_date);
END;
$$;

CREATE FUNCTION quest_refresh_workout_schedule() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    owner_id BIGINT := CASE WHEN TG_OP = 'DELETE' THEN OLD.user_id ELSE NEW.user_id END;
    zone TEXT := quest_user_timezone(owner_id);
BEGIN
    -- The BEFORE trigger already locks the user and snapshots unopened days with the old schedule.
    PERFORM quest_evaluate_day(owner_id, (clock_timestamp() AT TIME ZONE zone)::date);
    RETURN NULL;
END;
$$;

CREATE TRIGGER quest_workout_schedule_refresh
AFTER INSERT OR UPDATE OF workout_type, weekday OR DELETE ON fitness_workout_schedule
FOR EACH ROW EXECUTE FUNCTION quest_refresh_workout_schedule();

COMMIT;
