package dev.abhishekan.pulse.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class MealPreferencesTest {
    private val valid = MealPreferences(goal = "feel_satisfied", dietaryPattern = "no_preference")

    @Test fun everyWebAnswerCodeIsSupportedAndDefaultsMatch() {
        for (goal in MealPreferenceChoices.goals) {
            for (diet in MealPreferenceChoices.diets) {
                assertNull(
                    valid
                        .copy(
                            goal = goal,
                            dietaryPattern = diet,
                            barriers = MealPreferenceChoices.barriers,
                            foodAvoidances = MealPreferenceChoices.avoidances,
                        ).invalidStep(),
                )
            }
        }
        assertEquals(listOf("breakfast", "lunch", "dinner"), valid.meals.map { it.kind })
        assertEquals(listOf("09:00", "12:30", "19:30"), valid.meals.map { it.time.toString() })
    }

    @Test fun missingUnknownAndDuplicateValuesAreRejected() {
        assertEquals(0, MealPreferences().invalidStep())
        assertEquals(1, valid.copy(barriers = listOf("too_tired", "too_tired")).invalidStep())
        assertEquals(2, valid.copy(dietaryPattern = "unknown").invalidStep())
        assertEquals(3, valid.copy(foodAvoidances = listOf("unknown")).invalidStep())
        assertEquals(4, valid.copy(meals = emptyList()).invalidStep())
        assertEquals(4, valid.copy(meals = listOf(valid.meals.first(), valid.meals.first())).invalidStep())
        assertEquals(5, valid.copy(meals = listOf(MealPreferenceTime("lunch", LocalTime.of(12, 30, 1)))).invalidStep())
    }
}
