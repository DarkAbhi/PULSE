-- name: ListCatalogRecipes :many
SELECT id, name, dish_type, is_ready_to_eat, ready_in_minutes
FROM catalog_recipes
WHERE strpos(lower(name), lower(sqlc.arg(search)::text)) > 0
   OR strpos(lower(dish_type), lower(sqlc.arg(search)::text)) > 0
   OR EXISTS (SELECT 1 FROM unnest(alternate_names) AS alias WHERE strpos(lower(alias), lower(sqlc.arg(search)::text)) > 0)
   OR EXISTS (
       SELECT 1 FROM catalog_recipe_label_links AS link JOIN catalog_recipe_labels AS label ON label.id = link.label_id
       WHERE link.recipe_id = catalog_recipes.id AND label.kind IN ('tag', 'category', 'collection', 'meal_time')
         AND strpos(lower(label.name), lower(sqlc.arg(search)::text)) > 0
   )
ORDER BY (lower(name) = lower(sqlc.arg(search)::text)) DESC,
         (strpos(lower(name), lower(sqlc.arg(search)::text)) = 1) DESC,
         lower(name), id
LIMIT sqlc.arg(page_size) OFFSET sqlc.arg(page_offset);

-- name: GetCatalogRecipeName :one
SELECT name FROM catalog_recipes WHERE id = $1;

-- name: GetCatalogRecipe :one
SELECT r.id, r.name, r.description, r.dish_type, r.is_ready_to_eat, r.alternate_names, r.ready_in_minutes, r.details,
       COALESCE((
           SELECT jsonb_agg(jsonb_build_object(
               'name', a.name, 'quantity', a.quantity, 'unit', a.unit, 'important', a.important, 'ingredient_id', a.ingredient_id
           ) ORDER BY a.position)
           FROM catalog_recipe_ingredient_amounts AS a WHERE a.recipe_id = r.id
       ), '[]'::jsonb)::jsonb AS ingredients,
       COALESCE((
           SELECT jsonb_agg(jsonb_build_object(
               'id', s.id, 'name', s.name, 'instructions', s.instructions, 'type', s.step_type, 'details', s.details,
               'ingredients', COALESCE((
                   SELECT jsonb_agg(jsonb_build_object(
                       'name', a.name, 'quantity', a.quantity, 'unit', a.unit, 'important', a.important, 'ingredient_id', a.ingredient_id
                   ) ORDER BY a.position)
                   FROM catalog_step_ingredient_amounts AS a WHERE a.step_id = s.id
               ), '[]'::jsonb),
               'equipment', COALESCE((
                   SELECT jsonb_agg(l.name ORDER BY e.position)
                   FROM catalog_recipe_step_equipment AS e JOIN catalog_recipe_labels AS l ON e.label_id = l.id
                   WHERE e.step_id = s.id
               ), '[]'::jsonb)
           ) ORDER BY s.position)
           FROM catalog_recipe_steps AS s WHERE s.recipe_id = r.id
       ), '[]'::jsonb)::jsonb AS steps,
       COALESCE((
           SELECT jsonb_agg(jsonb_build_object('kind', l.kind, 'name', l.name) ORDER BY l.kind, rl.position, l.name)
           FROM catalog_recipe_label_links AS rl JOIN catalog_recipe_labels AS l ON rl.label_id = l.id
           WHERE rl.recipe_id = r.id
       ), '[]'::jsonb)::jsonb AS labels,
       COALESCE((
           SELECT jsonb_agg(jsonb_build_object('kind', link.kind, 'id', target.id, 'name', target.name)
                            ORDER BY link.kind, link.position)
           FROM catalog_recipe_links AS link JOIN catalog_recipes AS target ON link.target_id = target.id
           WHERE link.recipe_id = r.id
       ), '[]'::jsonb)::jsonb AS related
FROM catalog_recipes AS r
WHERE r.id = $1;
