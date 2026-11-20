package dev.abhishekan.pulse.feature.mealplan.planner

import dev.abhishekan.pulse.core.domain.model.MealPreferences
import dev.abhishekan.pulse.core.domain.model.MealTime
import dev.abhishekan.pulse.core.domain.model.PlannedMeal
import dev.abhishekan.pulse.feature.mealplan.editor.MealDraft
import java.time.LocalDate
import java.time.ZoneId

internal sealed interface MealPlanAction {
    data class Enter(
        val timeZone: ZoneId,
    ) : MealPlanAction

    data object Resume : MealPlanAction

    data object Refresh : MealPlanAction

    data object RefreshMeals : MealPlanAction

    data object RefreshTimes : MealPlanAction

    data object Today : MealPlanAction

    data class SelectDate(
        val date: LocalDate,
    ) : MealPlanAction

    data class ShiftWeek(
        val weeks: Long,
    ) : MealPlanAction

    data object AddMeal : MealPlanAction

    data class EditMeal(
        val meal: PlannedMeal,
    ) : MealPlanAction

    data object AddMealTime : MealPlanAction

    data class UpdateDraft(
        val draft: MealDraft,
    ) : MealPlanAction

    data object Discard : MealPlanAction

    data class Save(
        val preferences: MealPreferences? = null,
    ) : MealPlanAction

    data class DeleteMeal(
        val id: Long,
    ) : MealPlanAction

    data class DeleteMealTime(
        val time: MealTime,
    ) : MealPlanAction

    data class ToggleConsumed(
        val meal: PlannedMeal,
    ) : MealPlanAction

    data object Reviewed : MealPlanAction
}
