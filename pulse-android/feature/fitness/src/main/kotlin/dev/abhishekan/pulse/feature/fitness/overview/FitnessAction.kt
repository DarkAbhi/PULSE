package dev.abhishekan.pulse.feature.fitness.overview

import dev.abhishekan.pulse.core.domain.model.WorkoutClassification
import java.time.DayOfWeek
import java.time.LocalDate

sealed interface FitnessAction {
    data class SelectDate(
        val date: LocalDate,
    ) : FitnessAction

    data class ShiftMonth(
        val offset: Long,
    ) : FitnessAction

    data object SelectToday : FitnessAction

    data object Refresh : FitnessAction

    data object RefreshSchedule : FitnessAction

    data class SaveScheduleDay(
        val day: DayOfWeek,
        val classification: WorkoutClassification,
    ) : FitnessAction

    data object RecordWorkout : FitnessAction

    data object AcknowledgeReview : FitnessAction

    data object Resume : FitnessAction

    data object DismissMessage : FitnessAction
}
