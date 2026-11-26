CREATE TABLE catalog_recipes (
    id TEXT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    dish_type TEXT NOT NULL,
    is_ready_to_eat BOOLEAN NOT NULL,
    alternate_names TEXT[] NOT NULL,
    is_default BOOLEAN,
    ready_in_minutes INTEGER,
    details JSONB NOT NULL
);

CREATE INDEX catalog_recipes_name_idx ON catalog_recipes (lower(name), id);

CREATE TABLE catalog_recipe_labels (
    id TEXT PRIMARY KEY,
    kind TEXT NOT NULL CHECK (kind IN ('category', 'tag', 'meal_time', 'equipment', 'collection', 'ingredient_category', 'ingredient_storage', 'plan_category')),
    name TEXT NOT NULL,
    details JSONB NOT NULL
);

CREATE TABLE catalog_recipe_label_links (
    recipe_id TEXT NOT NULL REFERENCES catalog_recipes(id) ON DELETE CASCADE,
    label_id TEXT NOT NULL REFERENCES catalog_recipe_labels(id),
    position INTEGER NOT NULL,
    PRIMARY KEY (recipe_id, label_id)
);

CREATE TABLE catalog_ingredients (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    category_id TEXT REFERENCES catalog_recipe_labels(id),
    storage_id TEXT REFERENCES catalog_recipe_labels(id),
    details JSONB NOT NULL
);

CREATE TABLE catalog_recipe_steps (
    id TEXT PRIMARY KEY,
    recipe_id TEXT NOT NULL REFERENCES catalog_recipes(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    name TEXT NOT NULL,
    instructions TEXT NOT NULL,
    step_type TEXT NOT NULL,
    details JSONB NOT NULL,
    UNIQUE (recipe_id, position),
    UNIQUE (recipe_id, id)
);

CREATE TABLE catalog_recipe_step_links (
    recipe_id TEXT NOT NULL,
    step_id TEXT NOT NULL,
    target_id TEXT NOT NULL,
    kind TEXT NOT NULL CHECK (kind IN ('preceding', 'consecutive')),
    position INTEGER NOT NULL,
    PRIMARY KEY (step_id, kind, target_id),
    FOREIGN KEY (recipe_id, step_id) REFERENCES catalog_recipe_steps(recipe_id, id) ON DELETE CASCADE,
    FOREIGN KEY (recipe_id, target_id) REFERENCES catalog_recipe_steps(recipe_id, id) ON DELETE CASCADE,
    CHECK (step_id <> target_id)
);

CREATE INDEX catalog_recipe_step_links_target_idx ON catalog_recipe_step_links (recipe_id, target_id);

CREATE TABLE catalog_recipe_step_equipment (
    step_id TEXT NOT NULL REFERENCES catalog_recipe_steps(id) ON DELETE CASCADE,
    label_id TEXT NOT NULL REFERENCES catalog_recipe_labels(id),
    position INTEGER NOT NULL,
    PRIMARY KEY (step_id, label_id)
);

CREATE TABLE catalog_recipe_ingredient_amounts (
    recipe_id TEXT NOT NULL REFERENCES catalog_recipes(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    ingredient_id TEXT REFERENCES catalog_ingredients(id),
    name TEXT NOT NULL,
    quantity NUMERIC NOT NULL,
    unit TEXT NOT NULL,
    important BOOLEAN NOT NULL,
    PRIMARY KEY (recipe_id, position)
);

CREATE TABLE catalog_step_ingredient_amounts (
    step_id TEXT NOT NULL REFERENCES catalog_recipe_steps(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    ingredient_id TEXT REFERENCES catalog_ingredients(id),
    name TEXT NOT NULL,
    quantity NUMERIC NOT NULL,
    unit TEXT NOT NULL,
    important BOOLEAN NOT NULL,
    PRIMARY KEY (step_id, position)
);

CREATE TABLE catalog_recipe_links (
    recipe_id TEXT NOT NULL REFERENCES catalog_recipes(id) ON DELETE CASCADE,
    target_id TEXT NOT NULL REFERENCES catalog_recipes(id) ON DELETE CASCADE,
    kind TEXT NOT NULL CHECK (kind IN ('similar', 'pairing')),
    position INTEGER NOT NULL,
    PRIMARY KEY (recipe_id, kind, target_id),
    CHECK (recipe_id <> target_id)
);

CREATE INDEX catalog_recipe_links_target_idx ON catalog_recipe_links (target_id);

CREATE TABLE catalog_meal_templates (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    credited_author TEXT NOT NULL,
    category_id TEXT REFERENCES catalog_recipe_labels(id)
);

CREATE TABLE catalog_meal_template_recipes (
    template_id TEXT NOT NULL REFERENCES catalog_meal_templates(id) ON DELETE CASCADE,
    day_number INTEGER NOT NULL CHECK (day_number > 0),
    meal_time_id TEXT NOT NULL REFERENCES catalog_recipe_labels(id),
    position INTEGER NOT NULL,
    recipe_id TEXT NOT NULL REFERENCES catalog_recipes(id),
    PRIMARY KEY (template_id, day_number, meal_time_id, position)
);

ALTER TABLE meal_plans ADD COLUMN recipe_id TEXT REFERENCES catalog_recipes(id) ON DELETE SET NULL;
CREATE INDEX meal_plans_recipe_idx ON meal_plans (recipe_id) WHERE recipe_id IS NOT NULL;
