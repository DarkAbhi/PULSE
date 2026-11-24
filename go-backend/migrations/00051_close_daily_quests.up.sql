BEGIN;

ALTER FUNCTION quest_evaluate_day(BIGINT, DATE) RENAME TO quest_update_progress;

-- Backfilled calendar days still appear in history, but their progress is frozen.
CREATE FUNCTION quest_evaluate_day(owner_id BIGINT, quest_date DATE) RETURNS VOID LANGUAGE plpgsql AS $$
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
