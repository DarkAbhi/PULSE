-- Restore the schema expected by the previous API. Consolidated recipe rows and
-- saved meal links stay consolidated on rollback.
DROP INDEX catalog_recipes_name_idx;
CREATE INDEX catalog_recipes_name_idx ON catalog_recipes (lower(name), id);
