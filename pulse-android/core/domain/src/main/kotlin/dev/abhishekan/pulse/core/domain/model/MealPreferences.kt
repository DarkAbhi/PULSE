package dev.abhishekan.pulse.core.domain.model

import java.time.LocalTime

enum class MealOnboardingStatus { Pending, Skipped, Completed }

data class MealPreferenceTime(
    val kind: String,
    val time: LocalTime,
)

data class MealPreferences(
    val goal: String = "",
    val barriers: List<String> = emptyList(),
    val dietaryPattern: String = "",
    val foodAvoidances: List<String> = emptyList(),
    val meals: List<MealPreferenceTime> =
        listOf(
            MealPreferenceTime("breakfast", LocalTime.of(9, 0)),
            MealPreferenceTime("lunch", LocalTime.of(12, 30)),
            MealPreferenceTime("dinner", LocalTime.of(19, 30)),
        ),
) {
    fun invalidStep(): Int? =
        when {
            goal !in MealPreferenceChoices.goals -> 0
            barriers.distinct().size != barriers.size || barriers.any { it !in MealPreferenceChoices.barriers } -> 1
            dietaryPattern !in MealPreferenceChoices.diets -> 2
            foodAvoidances.distinct().size != foodAvoidances.size || foodAvoidances.any { it !in MealPreferenceChoices.avoidances } -> 3
            meals.isEmpty() ||
                meals.size > 3 ||
                meals.map { it.kind }.distinct().size != meals.size ||
                meals.any { it.kind !in MealPreferenceChoices.meals } -> 4
            meals.any { it.time.second != 0 || it.time.nano != 0 } -> 5
            else -> null
        }
}

object MealPreferenceChoices {
    val goals = listOf("feel_satisfied", "food_calm", "nourish_without_dieting")
    val barriers = listOf("hungry_again", "too_tired", "unsure_what_to_add", "food_advice_overwhelming")
    val diets = listOf("no_preference", "vegetarian", "vegan", "pescatarian")
    val avoidances =
        listOf(
            "shellfish",
            "fish",
            "peanuts",
            "tree_nuts",
            "dairy",
            "eggs",
            "gluten",
            "soy",
            "sesame",
            "pork",
            "beef",
            "poultry",
            "mushrooms",
            "onion_garlic",
            "spicy_food",
            "alcohol",
        )
    val meals = listOf("breakfast", "lunch", "dinner")
}

data class MealOnboarding(
    val status: MealOnboardingStatus,
    val preferences: MealPreferences? = null,
)
