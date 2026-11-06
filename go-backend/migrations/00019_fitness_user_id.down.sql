DROP INDEX idx_fitness_workouts_user_start_time;
ALTER TABLE fitness_workouts DROP CONSTRAINT fitness_workouts_user_uuid_key;
ALTER TABLE fitness_workouts ADD CONSTRAINT fitness_workouts_uuid_key UNIQUE (uuid);
CREATE INDEX idx_fitness_workouts_uuid ON fitness_workouts (uuid);
CREATE INDEX idx_fitness_workouts_start_time ON fitness_workouts (start_time DESC);

ALTER TABLE fitness_activity_rings DROP CONSTRAINT fitness_activity_rings_user_date_key;
ALTER TABLE fitness_activity_rings
    ADD CONSTRAINT fitness_activity_rings_summary_date_key UNIQUE (summary_date);
CREATE INDEX idx_fitness_activity_rings_summary_date
    ON fitness_activity_rings (summary_date DESC);

ALTER TABLE fitness_workouts DROP COLUMN user_id;
ALTER TABLE fitness_activity_rings DROP COLUMN user_id;
