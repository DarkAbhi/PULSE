BEGIN;

DROP TRIGGER quest_workout_schedule_refresh ON fitness_workout_schedule;
DROP FUNCTION quest_refresh_workout_schedule();
DROP FUNCTION quest_update_progress(BIGINT, DATE);
ALTER FUNCTION quest_measure_progress(BIGINT, DATE) RENAME TO quest_update_progress;

COMMIT;
