package dev.abhishekan.pulse.feature.mealplan.planner

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.MealPreferences
import dev.abhishekan.pulse.core.domain.model.MealTime
import dev.abhishekan.pulse.core.domain.model.PlannedMeal
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.feature.mealplan.editor.MealDraft
import java.time.LocalDate

internal sealed interface MealSection<out T> {
    val value: T?
    val loading: Boolean get() = this is Loading
    val error: Failure? get() = (this as? Failed)?.failure

    data class Loading<T>(
        override val value: T? = null,
    ) : MealSection<T>

    data class Ready<T>(
        override val value: T,
    ) : MealSection<T>

    data class Failed<T>(
        override val value: T?,
        val failure: Failure,
    ) : MealSection<T>
}

internal sealed interface MealWriteState {
    data object Idle : MealWriteState

    data object Saving : MealWriteState

    data class NeedsReview(
        val failure: Failure? = null,
    ) : MealWriteState
}

internal data class MealPlanUiState(
    val today: LocalDate,
    val selected: LocalDate,
    val week: LocalDate,
    val meals: MealSection<List<PlannedMeal>> = MealSection.Loading(),
    val times: MealSection<List<MealTime>> = MealSection.Loading(),
    val draft: MealDraft? = null,
    val write: MealWriteState = MealWriteState.Idle,
    val error: UiText? = null,
) {
    val busy get() = write == MealWriteState.Saving
    val reviewRequired get() = write is MealWriteState.NeedsReview
    val canWrite get() = write == MealWriteState.Idle
    val reviewReady get() = reviewRequired && meals is MealSection.Ready && times is MealSection.Ready

    fun selectableTimes(preferences: MealPreferences?): List<MealTime> =
        times.value.orEmpty().filter { time ->
            time.id == draft?.originalTimeId || preferences?.allows(time) != false
        }
}
