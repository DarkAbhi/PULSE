package dev.abhishekan.pulse.feature.fitness

import dev.abhishekan.pulse.core.domain.model.*

data class SetDraft(val reps: String = "", val weight: String = "")
data class ExerciseDraft(val exerciseId: Long? = null, val name: String = "", val sets: List<SetDraft> = listOf(SetDraft()),
    val catalogId: String? = null, val rememberAlias: Boolean = false)
data class ExerciseEditorState(val draft: ExerciseDraft, val dirty: Boolean = false, val error: String? = null,
    val candidates: List<CatalogExercise> = emptyList(), val searching: Boolean = false, val searchError: String? = null)
data class WorkoutUiState(val detail: WorkoutDetail? = null, val loading: Boolean = true, val error: Failure? = null,
    val busy: Boolean = false, val message: String? = null, val editor: ExerciseEditorState? = null,
    val needsReview: Boolean = false, val reviewReady: Boolean = false, val deleted: Boolean = false)
