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
            MealPreferenceTime("evening_snacks", LocalTime.of(17, 30)),
            MealPreferenceTime("dinner", LocalTime.of(19, 30)),
        ),
    val planningSlotsConfigured: Boolean = false,
) {
    fun allows(time: MealTime): Boolean {
        if (!planningSlotsConfigured || !time.isDefault) return true
        val kind = when (time.name) {
            "Breakfast" -> "breakfast"
            "Lunch" -> "lunch"
            "Evening Snacks" -> "evening_snacks"
            "Dinner" -> "dinner"
            else -> return true
        }
        return meals.any { it.kind == kind }
    }

    fun invalidStep(): Int? =
        when {
            goal !in MealPreferenceChoices.goals -> 0
            barriers.distinct().size != barriers.size || barriers.any { it !in MealPreferenceChoices.barriers } -> 1
            dietaryPattern !in MealPreferenceChoices.diets -> 2
            foodAvoidances.distinct().size != foodAvoidances.size || foodAvoidances.any { it !in MealPreferenceChoices.avoidances } -> 3
            meals.isEmpty() ||
                meals.size > MealPreferenceChoices.meals.size ||
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
    val meals = listOf("breakfast", "lunch", "evening_snacks", "dinner")
}

data class MealOnboarding(
    val status: MealOnboardingStatus,
    val preferences: MealPreferences? = null,
)
