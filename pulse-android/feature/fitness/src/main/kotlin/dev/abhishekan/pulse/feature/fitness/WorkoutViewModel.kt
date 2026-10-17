package dev.abhishekan.pulse.feature.fitness
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.domain.model.ExerciseInput
import dev.abhishekan.pulse.core.domain.model.ExerciseIssue
import dev.abhishekan.pulse.core.domain.model.ExerciseSetInput
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.WorkoutExercise
import dev.abhishekan.pulse.core.domain.usecase.DeleteExercise
import dev.abhishekan.pulse.core.domain.usecase.DeleteWorkout
import dev.abhishekan.pulse.core.domain.usecase.GetWorkoutDetail
import dev.abhishekan.pulse.core.domain.usecase.SaveExercise
import dev.abhishekan.pulse.core.domain.usecase.SearchExercises
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.fitness.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.isActive
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkoutViewModel(
    private val getWorkout: GetWorkoutDetail,
    private val search: SearchExercises,
    private val saveExercise: SaveExercise,
    private val deleteWorkout: DeleteWorkout,
    private val deleteExercise: DeleteExercise,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val id = checkNotNull(saved.get<Long>("id"))
    private val state =
        MutableStateFlow(
            WorkoutUiState(
                needsReview =
                    saved.get<String>("pending") != null,
            ),
        )
    val uiState = state.asStateFlow()
    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var original = ExerciseDraft()

    init {
        if (saved.get<Boolean>("editing") == true) restoreEditor()
        refresh()
    }

    fun refresh() {
        if (state.value.busy || loadJob?.isActive == true) return
        loadJob = viewModelScope.launch { load() }
    }

    private suspend fun load() {
        state.update { it.copy(loading = true, error = null, reviewReady = false) }
        when (val result = getWorkout(id)) {
            is Outcome.Success -> {
                state.update {
                    it.copy(detail = result.value, loading = false, reviewReady = it.needsReview)
                }
                if (saved.get<Boolean>("editing") == true) restoreEditor()
            }
            is Outcome.Error ->
                state.update {
                    it.copy(
                        loading = false,
                        error = result.failure,
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

    fun edit(exerciseId: Long? = null) {
        if (state.value.busy || state.value.loading || state.value.needsReview) return
        original =
            state.value.detail
                ?.exercises
                ?.firstOrNull { it.id == exerciseId }
                ?.draft()
                ?: ExerciseDraft()
        saved["editing"] = true
        state.update { it.copy(editor = ExerciseEditorState(original), message = null) }
        persistDraft(original)
    }

    private fun restoreEditor() {
        val exerciseId = saved.get<Long>("exerciseId")
        original =
            state.value.detail
                ?.exercises
                ?.firstOrNull { it.id == exerciseId }
                ?.draft()
                ?: ExerciseDraft()
        val reps =
            saved.get<ArrayList<String>>("reps")?.takeIf { it.isNotEmpty() } ?: arrayListOf("")
        val weights = saved.get<ArrayList<String>>("weights") ?: arrayListOf("")
        val draft =
            ExerciseDraft(
                exerciseId,
                saved["name"] ?: "",
                reps.mapIndexed { i, value ->
                    SetDraft(value, weights.getOrElse(i) { "" })
                },
                saved["catalogId"],
                saved["alias"] ?: false,
            )
        state.update { current ->
            current.copy(
                editor =
                    ExerciseEditorState(
                        draft,
                        saved["dirty"] ?: (draft != original),
                        error = current.editor?.error,
                        candidates = current.editor?.candidates.orEmpty(),
                        searching =
                            current.editor?.searching ?: false,
                        searchError = current.editor?.searchError,
                    ),
            )
        }
    }

    private fun persistDraft(draft: ExerciseDraft) {
        saved["dirty"] = draft != original
        saved["exerciseId"] = draft.exerciseId
        saved["name"] = draft.name
        saved["reps"] = ArrayList(draft.sets.map { it.reps })
        saved["weights"] =
            ArrayList(draft.sets.map { it.weight })
        saved["catalogId"] = draft.catalogId
        saved["alias"] = draft.rememberAlias
    }

    private fun updateDraft(transform: (ExerciseDraft) -> ExerciseDraft) {
        val editor = state.value.editor ?: return
        if (state.value.busy) return
        val draft = transform(editor.draft)
        persistDraft(draft)
        state.update {
            it.copy(editor = editor.copy(draft = draft, dirty = draft != original, error = null))
        }
    }

    fun nameChanged(name: String) {
        updateDraft { it.copy(name = name) }
        if (state.value.editor
                ?.draft
                ?.catalogId == null
        ) {
            findMatches()
        }
    }

    fun setChanged(
        index: Int,
        reps: String? = null,
        weight: String? = null,
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

    fun addSet() =
        updateDraft {
            if (it.sets.size <
                20
            ) {
                it.copy(sets = it.sets + SetDraft())
            } else {
                it
            }
        }

    fun removeSet(index: Int) =
        updateDraft {
            if (it.sets.size >
                1
            ) {
                it.copy(sets = it.sets.filterIndexed { i, _ -> i != index })
            } else {
                it
            }
        }

    fun rememberAlias(value: Boolean) = updateDraft { it.copy(rememberAlias = value) }

    fun choose(candidate: CatalogExercise) {
        searchJob?.cancel()
        updateDraft {
            it.copy(name = candidate.name, catalogId = candidate.id, rememberAlias = false)
        }
        state.update { it.copy(editor = it.editor?.copy(searching = false)) }
    }

    fun clearMatch() {
        updateDraft { it.copy(catalogId = null, rememberAlias = false) }
        findMatches()
    }

    fun findMatches() {
        searchJob?.cancel()
        val query =
            state.value.editor
                ?.draft
                ?.name ?: return
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
        state.update { it.copy(editor = it.editor?.copy(searching = true, searchError = null)) }
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

    fun closeEditor() {
        if (state.value.busy) return
        clearEditor()
    }

    private fun clearEditor() {
        searchJob?.cancel()
        listOf(
            "editing",
            "dirty",
            "exerciseId",
            "name",
            "reps",
            "weights",
            "catalogId",
            "alias",
        ).forEach {
            saved.remove<Any?>(it)
        }
        state.update { it.copy(editor = null) }
    }

    fun save() {
        val editor = state.value.editor ?: return
        if (state.value.busy || state.value.needsReview || state.value.loading) return
        val draft = editor.draft
        val sets =
            draft.sets.map {
                ExerciseSetInput(
                    it.reps.toIntOrNull() ?: 0,
                    if (it.weight.isBlank()) null else it.weight.trim().toBigDecimalOrNull(),
                )
            }
        val input = ExerciseInput(draft.name, sets, draft.catalogId, draft.rememberAlias)
        val error =
            when {
                draft.sets.any {
                    it.weight.isNotBlank() && it.weight.trim().toBigDecimalOrNull() == null
                } -> ExerciseIssue.Weight.message()
                input.issue != null -> checkNotNull(input.issue).message()
                else -> null
            }
        if (error != null) {
            state.update { it.copy(editor = editor.copy(error = error)) }
            return
        }
        mutate("save", { saveExercise(id, draft.exerciseId, input) }) {
            clearEditor()
            state.update { it.copy(message = uiText(R.string.pulse_fitness_exercise_saved)) }
        }
    }

    fun removeWorkout() =
        mutate("deleteWorkout", {
            deleteWorkout(id)
        }) { state.update { it.copy(deleted = true) } }

    fun removeExercise(exerciseId: Long) =
        mutate("deleteExercise", {
            deleteExercise(id, exerciseId)
        }) { state.update { it.copy(message = uiText(R.string.pulse_fitness_exercise_removed)) } }

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
                        val uncertain =
                            result.failure !in
                                setOf(Failure.InvalidInput, Failure.Unauthorized, Failure.NotFound)
                        if (!uncertain) saved.remove<String>("pending")
                        state.update {
                            it.copy(
                                needsReview = uncertain,
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

    fun acknowledgeReview() {
        if (!state.value.reviewReady || state.value.busy) return
        saved.remove<String>("pending")
        state.update { it.copy(needsReview = false, reviewReady = false, message = null) }
    }
}
