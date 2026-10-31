BEGIN;

ALTER TABLE vehicles ADD COLUMN user_id BIGINT;
ALTER TABLE gym_visits ADD COLUMN user_id BIGINT;

DO $$
DECLARE
    owner_id BIGINT;
BEGIN
    SELECT id INTO STRICT owner_id FROM users;
    UPDATE vehicles SET user_id = owner_id;
    UPDATE gym_visits SET user_id = owner_id;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE EXCEPTION 'ownership backfill requires exactly one user (found none)';
    WHEN TOO_MANY_ROWS THEN
        RAISE EXCEPTION 'ownership backfill requires exactly one user (found multiple)';
END $$;

ALTER TABLE vehicles
    ALTER COLUMN user_id SET NOT NULL,
    ADD CONSTRAINT vehicles_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE gym_visits
    ALTER COLUMN user_id SET NOT NULL,
    ADD CONSTRAINT gym_visits_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX vehicles_user_id_id_idx ON vehicles (user_id, id);
CREATE INDEX gym_visits_user_created_idx ON gym_visits (user_id, created_at DESC, id DESC);

COMMIT;
