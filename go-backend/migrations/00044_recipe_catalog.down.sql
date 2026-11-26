ALTER TABLE meal_plans DROP COLUMN recipe_id;
DROP TABLE catalog_meal_template_recipes, catalog_meal_templates, catalog_recipe_links,
    catalog_step_ingredient_amounts, catalog_recipe_ingredient_amounts, catalog_recipe_step_equipment,
    catalog_recipe_step_links, catalog_recipe_steps, catalog_ingredients, catalog_recipe_label_links,
    catalog_recipe_labels, catalog_recipes;
