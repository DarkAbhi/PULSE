BEGIN;
CREATE TABLE vehicle_catalog (
    id TEXT PRIMARY KEY,
    kind TEXT NOT NULL CHECK (kind IN ('car','motorcycle','moped','van','truck','bus')),
    make_slug TEXT NOT NULL,
    make_name TEXT NOT NULL,
    model_slug TEXT NOT NULL,
    model_name TEXT NOT NULL,
    aliases TEXT NOT NULL DEFAULT '',
    former_ids TEXT NOT NULL DEFAULT '',
    UNIQUE (kind,make_slug,model_slug),
    UNIQUE (kind,id)
);
CREATE INDEX vehicle_catalog_make_idx ON vehicle_catalog(kind,make_slug);
CREATE INDEX vehicle_catalog_search_idx ON vehicle_catalog USING gin
    ((lower(make_name || ' ' || model_name || ' ' || aliases)) gin_trgm_ops);
CREATE TABLE vehicle_catalog_imports (
    version TEXT PRIMARY KEY,
    sha256 TEXT NOT NULL,
    model_count INTEGER NOT NULL,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Keep historical classification as storage only, for rollback and other backends.
-- Existing rows cannot be assigned a precise kind from a wheel grouping alone.
ALTER TABLE vehicles ALTER COLUMN vehicle_type SET DEFAULT 'two_wheeler';
ALTER TABLE vehicles
    ADD COLUMN kind TEXT NOT NULL DEFAULT 'unknown'
        CHECK (kind IN ('unknown','car','motorcycle','moped','van','truck','bus')),
    ADD COLUMN catalog_id TEXT,
    ADD COLUMN make_name TEXT NOT NULL DEFAULT '' CHECK (length(make_name) <= 200),
    ADD COLUMN model_name TEXT NOT NULL DEFAULT '' CHECK (length(model_name) <= 200),
    ADD CONSTRAINT vehicles_catalog_fk FOREIGN KEY (kind,catalog_id)
        REFERENCES vehicle_catalog(kind,id);
CREATE INDEX vehicles_catalog_idx ON vehicles(catalog_id);
COMMIT;
