package dev.abhishekan.pulse.feature.fitness.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.WorkoutExercise
import dev.abhishekan.pulse.core.domain.usecase.DeleteExercise
import dev.abhishekan.pulse.core.domain.usecase.DeleteWorkout
import dev.abhishekan.pulse.core.domain.usecase.GetWorkoutDetail
import dev.abhishekan.pulse.core.domain.usecase.SaveExercise
import dev.abhishekan.pulse.core.domain.usecase.SearchExercises
import dev.abhishekan.pulse.core.domain.usecase.UpdateWorkoutClassification
import dev.abhishekan.pulse.core.ui.DraftSource
import dev.abhishekan.pulse.core.ui.UnsavedChanges
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.fitness.R
import dev.abhishekan.pulse.feature.fitness.shared.FitnessLoadState
import dev.abhishekan.pulse.feature.fitness.shared.FitnessReviewState
import dev.abhishekan.pulse.feature.fitness.shared.fitnessFailureMessage
import dev.abhishekan.pulse.feature.fitness.shared.requiresFitnessReview
import dev.abhishekan.pulse.feature.fitness.workout.editor.ExerciseDraft
import dev.abhishekan.pulse.feature.fitness.workout.editor.ExerciseDraftSavedState
import dev.abhishekan.pulse.feature.fitness.workout.editor.ExerciseEditorState
import dev.abhishekan.pulse.feature.fitness.workout.editor.SetDraft
import dev.abhishekan.pulse.feature.fitness.workout.editor.message
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkoutViewModel(
    private val getWorkout: GetWorkoutDetail,
    private val search: SearchExercises,
    private val saveExercise: SaveExercise,
    private val deleteWorkout: DeleteWorkout,
    private val deleteExercise: DeleteExercise,
    private val saved: SavedStateHandle,
    private val updateClassification: UpdateWorkoutClassification,
    unsavedChanges: UnsavedChanges = UnsavedChanges(),
) : ViewModel() {
    private val id = checkNotNull(saved.get<Long>("id"))
    private val draftSavedState = ExerciseDraftSavedState(saved)
    private val state =
        MutableStateFlow(
            WorkoutUiState(
                editor = draftSavedState.restore(),
                reviewState =
                    if (saved.get<String>("pending") != null) FitnessReviewState.AwaitingRefresh else FitnessReviewState.None,
            ),
        )
    val uiState = state.asStateFlow()

    init {
        unsavedChanges.track(DraftSource.Fitness, state.map { it.editor?.dirty == true }, viewModelScope)
    }

    fun onAction(action: WorkoutAction) {
        when (action) {
            WorkoutAction.Refresh -> refresh()
            is WorkoutAction.EditExercise -> edit(action.exerciseId)
            WorkoutAction.DeleteWorkout -> removeWorkout()
            is WorkoutAction.DeleteExercise -> removeExercise(action.exerciseId)
            WorkoutAction.AcknowledgeReview -> acknowledgeReview()
            is WorkoutAction.ChangeName -> nameChanged(action.name)
            is WorkoutAction.ChangeSet -> setChanged(action.index, action.reps, action.weight)
            WorkoutAction.AddSet -> addSet()
            is WorkoutAction.RemoveSet -> removeSet(action.index)
            is WorkoutAction.ChooseMatch -> choose(action.candidate)
            WorkoutAction.ClearMatch -> clearMatch()
            is WorkoutAction.SearchCatalogue -> searchCatalogue(action.query)
            is WorkoutAction.RememberAlias -> rememberAlias(action.value)
            WorkoutAction.QueueExercise -> queueExercise()
            is WorkoutAction.RemoveQueuedExercise -> removeQueuedExercise(action.index)
            WorkoutAction.SaveExercise -> save()
            is WorkoutAction.UpdateClassification -> {
                if (state.value.canChangeWorkout) {
                    mutate("classification", { updateClassification(id, action.classification) }) {}
                }
            }
            WorkoutAction.CloseEditor -> closeEditor()
            WorkoutAction.Resume -> resume()
            WorkoutAction.DismissMessage -> consumeMessage()
        }
    }

    private fun consumeMessage() {
        state.update { it.copy(message = null) }
    }

    private var loadJob: Job? = null
    private var hasResumed = false
    private var searchJob: Job? = null
    private var original: ExerciseDraft? =
        if (state.value.editor
                ?.draft
                ?.exerciseId == null
        ) {
            ExerciseDraft()
        } else {
            null
        }

    init {
        refresh()
    }

    private fun resume() {
        // Initialization owns the first load; later resumes revalidate the existing detail.
        if (hasResumed) refresh() else hasResumed = true
    }

    private fun refresh() {
        if (state.value.busy || loadJob?.isActive == true) return
        loadJob = viewModelScope.launch { load() }
    }

    private suspend fun load() {
        state.update {
            it.copy(
                loadState = FitnessLoadState.Loading(),
                reviewState = it.reviewState.afterRefresh(succeeded = false),
            )
        }
        when (val result = getWorkout(id)) {
            is Outcome.Success -> {
                val editor = state.value.editor
                val exercise = editor?.draft?.exerciseId?.let { id -> result.value.exercises.firstOrNull { it.id == id } }
                if (original == null) original = exercise?.draft()
                val catalog = exercise?.catalog?.takeIf { it.id == editor.draft.catalogId }
                state.update {
                    it.copy(
                        detail = result.value,
                        loadState = FitnessLoadState.Ready,
                        reviewState = it.reviewState.afterRefresh(succeeded = true),
                        editor =
                            if (editor != null && editor.selectedCatalog == null && catalog != null) {
                                editor.copy(selectedCatalog = catalog)
                            } else {
                                editor
                            },
                    )
                }
            }
            is Outcome.Error ->
                state.update {
                    it.copy(
                        loadState = FitnessLoadState.Failed(result.failure),
                        deleted =
                            result.failure == Failure.NotFound &&
                                saved.get<String>("pending") == "deleteWorkout",
                    )
                }
        }
    }

    private fun WorkoutExercise.draft() =
        ExerciseDraft(
            id,
            name,
            sets
                .map {
                    SetDraft(it.reps.toString(), it.weight?.toPlainString().orEmpty())
                }.ifEmpty { listOf(SetDraft()) },
            catalogId,
        )

    private fun edit(exerciseId: Long?) {
        val current = state.value
        if (!current.canChangeWorkout) return
        val exercise = exerciseId?.let { id -> current.detail?.exercises?.firstOrNull { it.id == id } ?: return }
        val draft = exercise?.draft() ?: ExerciseDraft()
        original = draft
        draftSavedState.save(draft, dirty = false)
        state.update { it.copy(editor = ExerciseEditorState(draft, selectedCatalog = exercise?.catalog), message = null) }
    }

    private fun updateDraft(transform: (ExerciseDraft) -> ExerciseDraft) {
        val editor = state.value.editor ?: return
        if (state.value.busy) return
        val draft = transform(editor.draft)
        val dirty = draft != original || editor.queued.isNotEmpty()
        draftSavedState.save(draft, dirty)
        state.update {
            it.copy(editor = editor.copy(draft = draft, dirty = dirty, error = null))
        }
    }

    private fun nameChanged(name: String) {
        updateDraft { it.copy(name = name) }
        if (state.value.editor
                ?.draft
                ?.catalogId == null
        ) {
            findMatches()
        }
    }

    private fun setChanged(
        index: Int,
        reps: String?,
        weight: String?,
    ) = updateDraft { draft ->
        draft.copy(
            sets =
                draft.sets.mapIndexed { i, set ->
                    if (i ==
                        index
                    ) {
                        SetDraft(reps ?: set.reps, weight ?: set.weight)
                    } else {
                        set
                    }
                },
        )
    }

    private fun addSet() =
        updateDraft {
            if (it.sets.size <
                20
            ) {
                it.copy(sets = it.sets + SetDraft())
            } else {
                it
            }
        }

    private fun removeSet(index: Int) =
        updateDraft {
            if (it.sets.size >
                1
            ) {
                it.copy(sets = it.sets.filterIndexed { i, _ -> i != index })
            } else {
                it
            }
        }

    private fun rememberAlias(value: Boolean) = updateDraft { it.copy(rememberAlias = value) }

    private fun choose(candidate: CatalogExercise) {
        if (state.value.busy || state.value.editor == null) return
        searchJob?.cancel()
        updateDraft {
            it.copy(name = candidate.name, catalogId = candidate.id, rememberAlias = false)
        }
        state.update { it.copy(editor = it.editor?.copy(searching = false, selectedCatalog = candidate)) }
    }

    private fun clearMatch() {
        if (state.value.busy || state.value.editor == null) return
        updateDraft { it.copy(catalogId = null, rememberAlias = false) }
        state.update { it.copy(editor = it.editor?.copy(selectedCatalog = null)) }
        findMatches()
    }

    private fun findMatches() {
        searchCatalogue(
            state.value.editor
                ?.draft
                ?.name ?: return,
        )
    }

    // Browsing a replacement must not mutate the current name, match, alias, or sets.
    private fun searchCatalogue(query: String) {
        if (state.value.editor == null || state.value.busy) return
        searchJob?.cancel()
        if (query.isBlank()) {
            state.update {
                it.copy(
                    editor =
                        it.editor?.copy(
                            candidates = emptyList(),
                            searching = false,
                            searchError = null,
                        ),
                )
            }
            return
        }
        state.update { it.copy(editor = it.editor?.copy(candidates = emptyList(), searching = true, searchError = null)) }
        searchJob =
            viewModelScope.launch {
                delay(300)
                when (val result = search(query)) {
                    is Outcome.Success ->
                        state.update {
                            it.copy(editor = it.editor?.copy(candidates = result.value, searching = false))
                        }
                    is Outcome.Error ->
                        state.update {
                            it.copy(
                                editor =
                                    it.editor?.copy(
                                        candidates = emptyList(),
                                        searching = false,
                                        searchError = uiText(R.string.pulse_fitness_catalogue_unavailable),
                                    ),
                            )
                        }
                }
            }
    }

    private fun closeEditor() {
        if (state.value.busy || state.value.editor == null) return
        clearEditor()
    }

    private fun clearEditor() {
        searchJob?.cancel()
        draftSavedState.clear()
        state.update { it.copy(editor = null) }
    }

    private fun queueExercise() {
        val editor = state.value.editor ?: return
        if (!editor.canQueue || state.value.busy || state.value.needsReview || state.value.loading) return
        searchJob?.cancel()
        val queue = editor.queued + editor.draft
        val draft = ExerciseDraft()
        original = draft
        draftSavedState.saveQueue(queue)
        draftSavedState.save(draft, dirty = true)
        state.update { it.copy(editor = ExerciseEditorState(draft, dirty = true, queued = queue)) }
    }

    private fun removeQueuedExercise(index: Int) {
        val editor = state.value.editor ?: return
        if (state.value.busy || state.value.needsReview || index !in editor.queued.indices) return
        val queue = editor.queued.filterIndexed { current, _ -> current != index }
        val dirty = queue.isNotEmpty() || editor.draft != original
        draftSavedState.saveQueue(queue)
        draftSavedState.save(editor.draft, dirty)
        state.update { it.copy(editor = editor.copy(queued = queue, dirty = dirty)) }
    }

    private fun save() {
        val editor = state.value.editor ?: return
        if (state.value.busy || state.value.needsReview || state.value.loading) return
        val draft = editor.draft
        if (editor.queued.isNotEmpty() && editor.canSave) {
            val drafts = editor.queued + listOf(draft).filter { it.issue == null }
            mutate("saveBatch", { saveExercise.batch(id, drafts.map { it.input }) }) {
                clearEditor()
                state.update { it.copy(message = uiText(R.string.pulse_fitness_exercises_saved)) }
            }
            return
        }
        val input = draft.input
        val error = draft.issue?.message()
        if (error != null) {
            state.update { it.copy(editor = editor.copy(error = error)) }
            return
        }
        mutate("save", { saveExercise(id, draft.exerciseId, input) }) {
            clearEditor()
            state.update { it.copy(message = uiText(R.string.pulse_fitness_exercise_saved)) }
        }
    }

    private fun removeWorkout() {
        if (!state.value.canChangeWorkout) return
        mutate("deleteWorkout", {
            deleteWorkout(id)
        }) { state.update { it.copy(deleted = true) } }
    }

    private fun removeExercise(exerciseId: Long) {
        if (!state.value.canChangeWorkout) return
        mutate("deleteExercise", {
            deleteExercise(id, exerciseId)
        }) { state.update { it.copy(message = uiText(R.string.pulse_fitness_exercise_removed)) } }
    }

    private fun mutate(
        kind: String,
        action: suspend () -> Outcome<Unit>,
        success: () -> Unit,
    ) {
        if (state.value.busy || state.value.loading || state.value.needsReview) return
        loadJob?.cancel()
        saved["pending"] = kind
        state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            when (val result = action()) {
                is Outcome.Success -> {
                    saved.remove<String>("pending")
                    success()
                    if (!state.value.deleted) load()
                }
                is Outcome.Error -> {
                    if (kind == "deleteWorkout" && result.failure == Failure.NotFound) {
                        saved.remove<String>("pending")
                        state.update { it.copy(deleted = true) }
                    } else {
                        val uncertain = result.failure.requiresFitnessReview
                        if (!uncertain) saved.remove<String>("pending")
                        state.update {
                            it.copy(
                                reviewState = if (uncertain) FitnessReviewState.AwaitingRefresh else FitnessReviewState.None,
                                message =
                                    if (uncertain) {
                                        uiText(R.string.pulse_fitness_uncertain_workout_change)
                                    } else {
                                        fitnessFailureMessage(result.failure)
                                    },
                            )
                        }
                        load()
                    }
                }
            }
            state.update { it.copy(busy = false) }
        }
    }

    private fun acknowledgeReview() {
        if (!state.value.reviewReady || state.value.busy) return
        saved.remove<String>("pending")
        state.update { it.copy(reviewState = FitnessReviewState.None, message = null) }
    }
}
