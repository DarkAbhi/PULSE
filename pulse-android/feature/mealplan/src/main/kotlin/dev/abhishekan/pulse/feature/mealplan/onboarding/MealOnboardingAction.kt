package dev.abhishekan.pulse.feature.mealplan.onboarding

import java.time.LocalTime

internal sealed interface MealOnboardingAction {
    data object Resume : MealOnboardingAction

    data object Refresh : MealOnboardingAction

    data class SelectGoal(
        val code: String,
    ) : MealOnboardingAction

    data class SelectDiet(
        val code: String,
    ) : MealOnboardingAction

    data class ToggleBarrier(
        val code: String,
    ) : MealOnboardingAction

    data class ToggleAvoidance(
        val code: String,
    ) : MealOnboardingAction

    data class SetMealEnabled(
        val kind: String,
        val enabled: Boolean,
    ) : MealOnboardingAction

    data class SetMealTime(
        val kind: String,
        val time: LocalTime,
    ) : MealOnboardingAction

    data object Next : MealOnboardingAction

    data object Back : MealOnboardingAction

    data class ReviewStep(
        val step: Int,
    ) : MealOnboardingAction

    data object EditPreferences : MealOnboardingAction

    data object CancelEditing : MealOnboardingAction

    data object Save : MealOnboardingAction

    data object Skip : MealOnboardingAction
}
