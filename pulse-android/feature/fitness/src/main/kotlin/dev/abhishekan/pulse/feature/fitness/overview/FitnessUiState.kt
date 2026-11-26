package dev.abhishekan.pulse.feature.fitness.overview

import dev.abhishekan.pulse.core.domain.model.FitnessOverview
import dev.abhishekan.pulse.core.domain.model.WorkoutScheduleDay
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.feature.fitness.shared.FitnessLoadState
import dev.abhishekan.pulse.feature.fitness.shared.FitnessReviewState
import java.time.LocalDate
import java.time.YearMonth

data class FitnessUiState(
    val selected: LocalDate,
    val month: YearMonth,
    val overview: FitnessOverview? = null,
    val loadState: FitnessLoadState = FitnessLoadState.Loading(),
    val busy: Boolean = false,
    val message: UiText? = null,
    val reviewState: FitnessReviewState = FitnessReviewState.None,
    val createdId: Long? = null,
    val schedule: List<WorkoutScheduleDay> = emptyList(),
    val scheduleState: FitnessLoadState = FitnessLoadState.Loading(),
    val savingSchedule: Boolean = false,
) {
    val loading get() = loadState is FitnessLoadState.Loading
    val error get() = loadState.error
    val needsReview get() = reviewState != FitnessReviewState.None
    val reviewReady get() = reviewState == FitnessReviewState.Ready
    val canRecord get() = overview != null && selected <= overview.today && error == null && !busy && !needsReview
}
