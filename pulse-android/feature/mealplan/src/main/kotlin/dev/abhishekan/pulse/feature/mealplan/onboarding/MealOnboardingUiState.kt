package dev.abhishekan.pulse.feature.mealplan.onboarding

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.MealOnboarding
import dev.abhishekan.pulse.core.domain.model.MealOnboardingStatus
import dev.abhishekan.pulse.core.domain.model.MealPreferences

internal data class MealOnboardingUiState(
    val record: MealOnboarding? = null,
    val error: Failure? = null,
    val draft: MealPreferences = MealPreferences(),
    val step: Int = 0,
    val editing: Boolean = false,
    val saving: Boolean = false,
    val invalidStep: Int? = null,
    val returnToReview: Boolean = false,
    val resolvedElsewhere: Boolean = false,
) {
    val showJourney get() = record?.status == MealOnboardingStatus.Pending || editing
}
