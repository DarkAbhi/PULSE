CREATE TABLE meal_plan_onboarding (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    status TEXT NOT NULL CHECK (status IN ('skipped', 'completed')),
    questionnaire_version INTEGER NOT NULL CHECK (questionnaire_version = 1),
    decided_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE meal_preferences (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    goal TEXT NOT NULL CHECK (goal IN ('feel_satisfied', 'food_calm', 'nourish_without_dieting')),
    dietary_pattern TEXT NOT NULL CHECK (dietary_pattern IN ('no_preference', 'vegetarian', 'vegan', 'pescatarian')),
    barriers TEXT[] NOT NULL DEFAULT '{}' CHECK (barriers <@ ARRAY['hungry_again', 'too_tired', 'unsure_what_to_add', 'food_advice_overwhelming']::TEXT[] AND array_position(barriers, NULL) IS NULL),
    food_avoidances TEXT[] NOT NULL DEFAULT '{}' CHECK (food_avoidances <@ ARRAY['shellfish','fish','peanuts','tree_nuts','dairy','eggs','gluten','soy','sesame','pork','beef','poultry','mushrooms','onion_garlic','spicy_food','alcohol']::TEXT[] AND array_position(food_avoidances, NULL) IS NULL),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE meal_preference_times (
    user_id BIGINT NOT NULL REFERENCES meal_preferences(user_id) ON DELETE CASCADE,
    meal_kind TEXT NOT NULL CHECK (meal_kind IN ('breakfast', 'lunch', 'dinner')),
    usual_time TIME WITHOUT TIME ZONE NOT NULL CHECK (usual_time < '24:00:00'::TIME AND EXTRACT(SECOND FROM usual_time) = 0),
    PRIMARY KEY (user_id, meal_kind)
);
