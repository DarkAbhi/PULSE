package dev.abhishekan.pulse.feature.fitness.workout

import dev.abhishekan.pulse.core.domain.model.CatalogExercise

sealed interface WorkoutAction {
    data object Refresh : WorkoutAction

    data class EditExercise(
        val exerciseId: Long? = null,
    ) : WorkoutAction

    data object DeleteWorkout : WorkoutAction

    data class DeleteExercise(
        val exerciseId: Long,
    ) : WorkoutAction

    data object AcknowledgeReview : WorkoutAction

    data class ChangeName(
        val name: String,
    ) : WorkoutAction

    data class ChangeSet(
        val index: Int,
        val reps: String?,
        val weight: String?,
    ) : WorkoutAction

    data object AddSet : WorkoutAction

    data class RemoveSet(
        val index: Int,
    ) : WorkoutAction

    data class ChooseMatch(
        val candidate: CatalogExercise,
    ) : WorkoutAction

    data object ClearMatch : WorkoutAction

    data class SearchCatalogue(
        val query: String,
    ) : WorkoutAction

    data class RememberAlias(
        val value: Boolean,
    ) : WorkoutAction

    data object SaveExercise : WorkoutAction

    data object CloseEditor : WorkoutAction

    data object Resume : WorkoutAction

    data object DismissMessage : WorkoutAction
}
