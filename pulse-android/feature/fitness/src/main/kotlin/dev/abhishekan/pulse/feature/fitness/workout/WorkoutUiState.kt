package dev.abhishekan.pulse.feature.fitness.workout

import dev.abhishekan.pulse.core.domain.model.WorkoutDetail
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.feature.fitness.shared.FitnessLoadState
import dev.abhishekan.pulse.feature.fitness.shared.FitnessReviewState
import dev.abhishekan.pulse.feature.fitness.workout.editor.ExerciseEditorState

data class WorkoutUiState(
    val detail: WorkoutDetail? = null,
    val loadState: FitnessLoadState = FitnessLoadState.Loading(),
    val busy: Boolean = false,
    val message: UiText? = null,
    val editor: ExerciseEditorState? = null,
    val reviewState: FitnessReviewState = FitnessReviewState.None,
    val deleted: Boolean = false,
) {
    val loading get() = loadState is FitnessLoadState.Loading
    val error get() = loadState.error
    val needsReview get() = reviewState != FitnessReviewState.None
    val reviewReady get() = reviewState == FitnessReviewState.Ready
    val canChangeWorkout get() = detail != null && loadState == FitnessLoadState.Ready && !busy && !needsReview && editor == null && !deleted
}
