package dev.abhishekan.pulse.feature.fitness.overview

import java.time.LocalDate

sealed interface FitnessAction {
    data class SelectDate(val date: LocalDate) : FitnessAction
    data class ShiftMonth(val offset: Long) : FitnessAction
    data object SelectToday : FitnessAction
    data object Refresh : FitnessAction
    data object RecordWorkout : FitnessAction
    data object AcknowledgeReview : FitnessAction
    data object Resume : FitnessAction
    data object DismissMessage : FitnessAction
}
