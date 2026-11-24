BEGIN;

DROP FUNCTION quest_evaluate_day(BIGINT, DATE);
ALTER FUNCTION quest_update_progress(BIGINT, DATE) RENAME TO quest_evaluate_day;

COMMIT;
