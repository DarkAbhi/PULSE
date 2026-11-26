DROP TABLE exercise_aliases;
ALTER TABLE gym_visit_exercises DROP COLUMN exercise_catalog_id;
DROP TABLE exercise_catalog;
-- pg_trgm may be shared by other applications; leave the extension installed.
