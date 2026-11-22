BEGIN;

LOCK TABLE catalog_recipes, meal_plans IN SHARE ROW EXCLUSIVE MODE;

-- The imported recipe IDs are MongoDB ObjectIds. Descending ID matches the source
-- creation timestamps for every same-name pair in the seed.
CREATE TEMP TABLE duplicate_catalog_recipes (
    retired_id TEXT PRIMARY KEY,
    retained_id TEXT NOT NULL
) ON COMMIT DROP;

INSERT INTO duplicate_catalog_recipes (retired_id, retained_id)
SELECT id, retained_id
FROM (
    SELECT id, first_value(id) OVER (
        PARTITION BY lower(btrim(name)) ORDER BY id COLLATE "C" DESC
    ) AS retained_id
    FROM catalog_recipes
) AS canonical
WHERE id <> retained_id;

UPDATE meal_plans AS meal
SET recipe_id = duplicate.retained_id
FROM duplicate_catalog_recipes AS duplicate
WHERE meal.recipe_id = duplicate.retired_id;

UPDATE catalog_meal_template_recipes AS meal
SET recipe_id = duplicate.retained_id
FROM duplicate_catalog_recipes AS duplicate
WHERE meal.recipe_id = duplicate.retired_id;

-- Collections curate dishes across the catalog. Keep their membership even
-- when the editor selected the older recipe. Other labels stay with the recipe.
INSERT INTO catalog_recipe_label_links (recipe_id, label_id, position)
SELECT duplicate.retained_id, link.label_id, min(link.position)
FROM catalog_recipe_label_links AS link
JOIN duplicate_catalog_recipes AS duplicate ON duplicate.retired_id = link.recipe_id
JOIN catalog_recipe_labels AS label ON label.id = link.label_id AND label.kind = 'collection'
GROUP BY duplicate.retained_id, link.label_id
ON CONFLICT (recipe_id, label_id) DO NOTHING;

-- Retarget links from surviving recipes. Leave the newer recipe's own steps,
-- ingredients, and pairing choices intact, and skip links back to itself.
INSERT INTO catalog_recipe_links (recipe_id, target_id, kind, position)
SELECT link.recipe_id, duplicate.retained_id, link.kind, min(link.position)
FROM catalog_recipe_links AS link
JOIN duplicate_catalog_recipes AS duplicate ON duplicate.retired_id = link.target_id
WHERE link.recipe_id <> duplicate.retained_id
  AND NOT EXISTS (SELECT 1 FROM duplicate_catalog_recipes AS source WHERE source.retired_id = link.recipe_id)
GROUP BY link.recipe_id, duplicate.retained_id, link.kind
ON CONFLICT (recipe_id, kind, target_id) DO NOTHING;

DELETE FROM catalog_recipes WHERE id IN (SELECT retired_id FROM duplicate_catalog_recipes);

DROP INDEX catalog_recipes_name_idx;
CREATE UNIQUE INDEX catalog_recipes_name_idx ON catalog_recipes (lower(btrim(name)));

COMMIT;
