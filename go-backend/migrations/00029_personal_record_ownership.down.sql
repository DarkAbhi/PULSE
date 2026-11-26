BEGIN;
ALTER TABLE sports DROP COLUMN user_id;
ALTER TABLE credit_cards DROP COLUMN user_id;
ALTER TABLE meditations DROP COLUMN user_id;
COMMIT;
