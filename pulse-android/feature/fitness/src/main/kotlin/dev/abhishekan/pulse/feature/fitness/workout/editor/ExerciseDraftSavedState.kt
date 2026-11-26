package dev.abhishekan.pulse.feature.fitness.workout.editor

import androidx.lifecycle.SavedStateHandle

internal class ExerciseDraftSavedState(
    private val saved: SavedStateHandle,
) {
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
        val queued = saved.get<ArrayList<ArrayList<String>>>("queued").orEmpty().mapNotNull { it.toDraft() }
        return ExerciseEditorState(draft, dirty = saved["dirty"] ?: (draft != ExerciseDraft()), queued = queued)
    }

    fun save(
        draft: ExerciseDraft,
        dirty: Boolean,
    ) {
        saved["exerciseId"] = draft.exerciseId
        saved["name"] = draft.name
        saved["reps"] = ArrayList(draft.sets.map { it.reps })
        saved["weights"] = ArrayList(draft.sets.map { it.weight })
        saved["catalogId"] = draft.catalogId
        saved["alias"] = draft.rememberAlias
        saved["dirty"] = dirty
        saved["editing"] = true
    }

    fun saveQueue(queue: List<ExerciseDraft>) {
        saved["queued"] =
            ArrayList(
                queue.map { draft ->
                    arrayListOf(draft.name, draft.catalogId.orEmpty(), draft.rememberAlias.toString()).apply {
                        draft.sets.forEach { set ->
                            add(set.reps)
                            add(set.weight)
                        }
                    }
                },
            )
    }

    private fun List<String>.toDraft(): ExerciseDraft? {
        if (size < 5 || (size - 3) % 2 != 0) return null
        return ExerciseDraft(
            name = this[0],
            sets = drop(3).chunked(2).map { SetDraft(it[0], it[1]) },
            catalogId = this[1].takeIf(String::isNotBlank),
            rememberAlias = this[2].toBoolean(),
        )
    }

    fun clear() {
        listOf("editing", "dirty", "exerciseId", "name", "reps", "weights", "catalogId", "alias", "queued").forEach {
            saved.remove<Any?>(it)
        }
    }
}
