BEGIN;
ALTER TABLE vehicles DROP COLUMN catalog_id, DROP COLUMN kind,
    DROP COLUMN make_name, DROP COLUMN model_name;
ALTER TABLE vehicles ALTER COLUMN vehicle_type DROP DEFAULT;
DROP TABLE vehicle_catalog_imports;
DROP TABLE vehicle_catalog;
COMMIT;
