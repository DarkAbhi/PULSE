package dev.abhishekan.pulse.feature.fitness.workout.editor

import androidx.lifecycle.SavedStateHandle

internal class ExerciseDraftSavedState(private val saved: SavedStateHandle) {
    fun restore(): ExerciseEditorState? {
        if (saved.get<Boolean>("editing") != true) return null
        val reps = saved.get<ArrayList<String>>("reps").orEmpty().ifEmpty { listOf("") }
        val weights = saved.get<ArrayList<String>>("weights").orEmpty()
        val draft =
            ExerciseDraft(
                exerciseId = saved["exerciseId"],
                name = saved["name"] ?: "",
                sets = reps.mapIndexed { index, value -> SetDraft(value, weights.getOrElse(index) { "" }) },
                catalogId = saved["catalogId"],
                rememberAlias = saved["alias"] ?: false,
            )
        return ExerciseEditorState(draft, dirty = saved["dirty"] ?: (draft != ExerciseDraft()))
    }

    fun save(draft: ExerciseDraft, dirty: Boolean) {
        saved["exerciseId"] = draft.exerciseId
        saved["name"] = draft.name
        saved["reps"] = ArrayList(draft.sets.map { it.reps })
        saved["weights"] = ArrayList(draft.sets.map { it.weight })
        saved["catalogId"] = draft.catalogId
        saved["alias"] = draft.rememberAlias
        saved["dirty"] = dirty
        saved["editing"] = true
    }

    fun clear() {
        listOf("editing", "dirty", "exerciseId", "name", "reps", "weights", "catalogId", "alias").forEach {
            saved.remove<Any?>(it)
        }
    }
}
