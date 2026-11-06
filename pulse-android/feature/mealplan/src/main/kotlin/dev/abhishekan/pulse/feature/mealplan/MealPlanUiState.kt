package dev.abhishekan.pulse.feature.mealplan
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.MealTime
import dev.abhishekan.pulse.core.domain.model.PlannedMeal
import dev.abhishekan.pulse.core.ui.UiText
import java.time.LocalDate

data class MealSection<T>(
    val value: T? = null,
    val loading: Boolean = true,
    val error: Failure? = null,
)

data class MealPlanUiState(
    val today: LocalDate,
    val selected: LocalDate,
    val week: LocalDate,
    val meals: MealSection<List<PlannedMeal>> = MealSection(),
    val times: MealSection<List<MealTime>> = MealSection(),
    val managing: Boolean = false,
    val draft: MealDraft? = null,
    val busy: Boolean = false,
    val reviewRequired: Boolean = false,
    val error: UiText? = null,
    val message: UiText? = null,
) {
    val canWrite get() = !busy && !reviewRequired
    val reviewReady get() =
        !meals.loading &&
            meals.error == null &&
            meals.value != null &&
            !times.loading &&
            times.error == null &&
            times.value != null
    val days get() = (0L..6L).map(week::plusDays)
}
