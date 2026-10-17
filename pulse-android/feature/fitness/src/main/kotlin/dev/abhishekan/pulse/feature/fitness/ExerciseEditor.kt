package dev.abhishekan.pulse.feature.fitness

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@Composable
internal fun ExerciseEditor(editor: ExerciseEditorState, busy: Boolean, needsReview: Boolean,
    onName: (String) -> Unit, onSet: (Int, String?, String?) -> Unit, onAddSet: () -> Unit, onRemoveSet: (Int) -> Unit,
    onChoose: (CatalogExercise) -> Unit, onClearMatch: () -> Unit, onSearch: () -> Unit, onAlias: (Boolean) -> Unit,
    onSave: () -> Unit, onCancel: () -> Unit) {
    val draft = editor.draft
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val repsFocus = remember(draft.sets.size) { List(draft.sets.size) { FocusRequester() } }
    val weightFocus = remember(draft.sets.size) { List(draft.sets.size) { FocusRequester() } }
    val next = KeyboardActions(onNext = { repsFocus.first().requestFocus() })
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stackFields = maxWidth / LocalDensity.current.fontScale < PulseTokens.formColumnsWidth
        PulseCard(Modifier.fillMaxWidth()) {
            PulseText(if (draft.exerciseId == null) "Add an exercise" else "Edit exercise", Modifier.semantics { heading() }, PulseTextStyle.Title)
            PulseText("Capture what you did, one set at a time.")
            PulseTextField(draft.name, onName, "Exercise name", Modifier.fillMaxWidth(), enabled = !busy,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = next)
            if (draft.catalogId != null) {
                PulseText("Catalogue match selected. You can use your own name for it.")
                editor.candidates.firstOrNull { it.id == draft.catalogId }?.let { CatalogInformation(it) }
                PulseTextButton("Change catalogue match", onClearMatch, enabled = !busy)
                PulseCheckbox(draft.rememberAlias, onAlias, "Remember this name for future workouts", enabled = !busy)
            } else {
                PulseText("Search by name above, or keep your own exercise name.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (editor.searching) PulseLoadingState("Finding exercises")
                editor.searchError?.let {
                    PulseText(it)
                    PulseTextButton("Retry catalogue search", onSearch, enabled = !busy)
                }
                if (draft.name.isNotBlank() && !editor.searching && editor.searchError == null && editor.candidates.isEmpty()) PulseText("No matches yet. You can save your own exercise name.")
                editor.candidates.forEach { candidate ->
                    PulseRadioButton(false, { onChoose(candidate) }, "${candidate.name} · ${candidate.equipment ?: "Equipment unspecified"}",
                        Modifier.fillMaxWidth(), enabled = !busy)
                }
            }
            PulseText("Your sets", Modifier.semantics { heading() }, PulseTextStyle.Title)
            PulseText("Repetitions: 1–1,000 per set. Leave weight blank for bodyweight. Up to 20 sets.")
            draft.sets.forEachIndexed { index, set ->
                PulseText("Set ${index + 1}", style = PulseTextStyle.Label)
                // Stack numeric fields when text is large so labels and typed values stay readable.
                if (stackFields) {
                    PulseTextField(set.reps, { onSet(index, it, null) }, "Reps, set ${index + 1}", Modifier.fillMaxWidth().focusRequester(repsFocus[index]), enabled = !busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { weightFocus[index].requestFocus() }))
                    WeightField(index, set.weight, busy, index == draft.sets.lastIndex, Modifier.focusRequester(weightFocus[index]),
                        { onSet(index, null, it) }, { repsFocus.getOrNull(index + 1)?.requestFocus() }, { keyboard?.hide(); focus.clearFocus() })
                } else Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                    PulseTextField(set.reps, { onSet(index, it, null) }, "Reps, set ${index + 1}", Modifier.weight(1f).focusRequester(repsFocus[index]), enabled = !busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { weightFocus[index].requestFocus() }))
                    Box(Modifier.weight(1f)) { WeightField(index, set.weight, busy, index == draft.sets.lastIndex, Modifier.focusRequester(weightFocus[index]),
                        { onSet(index, null, it) }, { repsFocus.getOrNull(index + 1)?.requestFocus() }, { keyboard?.hide(); focus.clearFocus() }) }
                }
                if (draft.sets.size > 1) PulseTextButton("Remove set ${index + 1}", { onRemoveSet(index) }, enabled = !busy)
            }
            PulseTextButton("Add another set", onAddSet, enabled = !busy && draft.sets.size < 20)
            editor.error?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.error) }
            PulseButton(if (draft.exerciseId == null) "Save exercise" else "Save changes", { keyboard?.hide(); onSave() }, Modifier.fillMaxWidth(),
                enabled = !needsReview, loading = busy)
            PulseTextButton("Cancel", onCancel, Modifier.fillMaxWidth(), enabled = !busy)
        }
    }
}

@Composable
private fun WeightField(index: Int, value: String, busy: Boolean, last: Boolean, modifier: Modifier, onValue: (String) -> Unit, onNext: () -> Unit, onDone: () -> Unit) {
    PulseTextField(value, onValue, "Weight (kg), set ${index + 1}", modifier.fillMaxWidth(), enabled = !busy,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = if (last) ImeAction.Done else ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onDone() }))
}
