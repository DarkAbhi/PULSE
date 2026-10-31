BEGIN;

DROP INDEX gym_visits_user_created_idx;
DROP INDEX vehicles_user_id_id_idx;
ALTER TABLE gym_visits DROP COLUMN user_id;
ALTER TABLE vehicles DROP COLUMN user_id;

COMMIT;
