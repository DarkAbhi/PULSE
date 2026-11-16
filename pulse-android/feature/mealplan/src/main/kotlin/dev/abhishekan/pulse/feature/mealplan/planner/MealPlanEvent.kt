package dev.abhishekan.pulse.feature.mealplan.planner

import dev.abhishekan.pulse.core.ui.UiText

internal sealed interface MealPlanEvent {
    data class ShowMessage(
        val message: UiText,
    ) : MealPlanEvent
}
