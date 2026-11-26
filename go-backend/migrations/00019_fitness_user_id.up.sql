DO $$
BEGIN
    IF (SELECT COUNT(*) FROM users) <> 1 THEN
        RAISE EXCEPTION 'fitness backfill requires exactly one user';
    END IF;
END $$;

ALTER TABLE fitness_activity_rings ADD COLUMN user_id BIGINT;
ALTER TABLE fitness_workouts ADD COLUMN user_id BIGINT;

UPDATE fitness_activity_rings SET user_id = (SELECT id FROM users);
UPDATE fitness_workouts SET user_id = (SELECT id FROM users);

ALTER TABLE fitness_activity_rings
    ALTER COLUMN user_id SET NOT NULL,
    ADD CONSTRAINT fitness_activity_rings_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE fitness_workouts
    ALTER COLUMN user_id SET NOT NULL,
    ADD CONSTRAINT fitness_workouts_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE fitness_activity_rings DROP CONSTRAINT fitness_activity_rings_summary_date_key;
ALTER TABLE fitness_activity_rings
    ADD CONSTRAINT fitness_activity_rings_user_date_key UNIQUE (user_id, summary_date);
DROP INDEX idx_fitness_activity_rings_summary_date;

ALTER TABLE fitness_workouts DROP CONSTRAINT fitness_workouts_uuid_key;
ALTER TABLE fitness_workouts
    ADD CONSTRAINT fitness_workouts_user_uuid_key UNIQUE (user_id, uuid);
DROP INDEX idx_fitness_workouts_uuid;
DROP INDEX idx_fitness_workouts_start_time;
CREATE INDEX idx_fitness_workouts_user_start_time
    ON fitness_workouts (user_id, start_time DESC);
