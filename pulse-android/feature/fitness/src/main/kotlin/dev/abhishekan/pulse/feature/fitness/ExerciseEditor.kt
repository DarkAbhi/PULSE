package dev.abhishekan.pulse.feature.fitness
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCheckbox
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.fitness.R

@Composable
internal fun ExerciseEditor(
    editor: ExerciseEditorState,
    busy: Boolean,
    needsReview: Boolean,
    onName: (String) -> Unit,
    onSet: (Int, String?, String?) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onChoose: (CatalogExercise) -> Unit,
    onClearMatch: () -> Unit,
    onSearch: () -> Unit,
    onAlias: (Boolean) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val draft = editor.draft
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val repsFocus = remember(draft.sets.size) { List(draft.sets.size) { FocusRequester() } }
    val weightFocus = remember(draft.sets.size) { List(draft.sets.size) { FocusRequester() } }
    val next = KeyboardActions(onNext = { repsFocus.first().requestFocus() })
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stackFields = maxWidth / LocalDensity.current.fontScale < PulseTokens.formColumnsWidth
        PulseCard(Modifier.fillMaxWidth()) {
            PulseText(
                if (draft.exerciseId ==
                    null
                ) {
                    stringResource(R.string.pulse_fitness_add_an_exercise)
                } else {
                    stringResource(R.string.pulse_fitness_edit_exercise)
                },
                Modifier.semantics { heading() },
                PulseTextStyle.Title,
            )
            PulseText(stringResource(R.string.pulse_fitness_capture_what_you_did_one_set_at_a_time))
            PulseTextField(
                draft.name,
                onName,
                stringResource(R.string.pulse_fitness_exercise_name),
                Modifier.fillMaxWidth(),
                enabled = !busy,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = next,
            )
            if (draft.catalogId != null) {
                PulseText(
                    stringResource(
                        R.string.pulse_fitness_catalogue_match_selected_you_can_use_your_own_name,
                    ),
                )
                editor.candidates
                    .firstOrNull {
                        it.id == draft.catalogId
                    }?.let { CatalogInformation(it) }
                PulseTextButton(
                    stringResource(R.string.pulse_fitness_change_catalogue_match),
                    onClearMatch,
                    enabled = !busy,
                )
                PulseCheckbox(
                    draft.rememberAlias,
                    onAlias,
                    stringResource(R.string.pulse_fitness_remember_this_name_for_future_workouts),
                    enabled = !busy,
                )
            } else {
                PulseText(
                    stringResource(
                        R.string.pulse_fitness_search_by_name_above_or_keep_your_own_exercise,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (editor.searching) {
                    PulseLoadingState(
                        stringResource(R.string.pulse_fitness_finding_exercises),
                    )
                }
                editor.searchError?.let {
                    PulseText(it)
                    PulseTextButton(
                        stringResource(R.string.pulse_fitness_retry_catalogue_search),
                        onSearch,
                        enabled = !busy,
                    )
                }
                if (draft.name.isNotBlank() &&
                    !editor.searching &&
                    editor.searchError == null &&
                    editor.candidates.isEmpty()
                ) {
                    PulseText(
                        stringResource(
                            R.string.pulse_fitness_no_matches_yet_you_can_save_your_own_exercise,
                        ),
                    )
                }
                editor.candidates.forEach { candidate ->
                    PulseRadioButton(
                        false,
                        {
                            onChoose(candidate)
                        },
                        stringResource(
                            R.string.pulse_fitness_exercise_equipment,
                            candidate.name,
                            candidate.equipment
                                ?: stringResource(R.string.pulse_fitness_equipment_unspecified),
                        ),
                        Modifier.fillMaxWidth(),
                        enabled = !busy,
                    )
                }
            }
            PulseText(
                stringResource(R.string.pulse_fitness_your_sets),
                Modifier.semantics {
                    heading()
                },
                PulseTextStyle.Title,
            )
            PulseText(
                stringResource(
                    R.string.pulse_fitness_repetitions_1_1_000_per_set_leave_weight_blank,
                ),
            )
            draft.sets.forEachIndexed { index, set ->
                PulseText(
                    stringResource(R.string.pulse_fitness_set_1_s, index + 1),
                    style = PulseTextStyle.Label,
                )
                // Stack numeric fields when text is large so labels and typed values stay readable.
                if (stackFields) {
                    PulseTextField(
                        set.reps,
                        {
                            onSet(index, it, null)
                        },
                        stringResource(
                            R.string.pulse_fitness_reps_set_1_s,
                            index + 1,
                        ),
                        Modifier.fillMaxWidth().focusRequester(repsFocus[index]),
                        enabled = !busy,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next,
                            ),
                        keyboardActions =
                            KeyboardActions(onNext = {
                                weightFocus[index].requestFocus()
                            }),
                    )
                    WeightField(
                        index,
                        set.weight,
                        busy,
                        index == draft.sets.lastIndex,
                        Modifier.focusRequester(weightFocus[index]),
                        {
                            onSet(index, null, it)
                        },
                        { repsFocus.getOrNull(index + 1)?.requestFocus() },
                        {
                            keyboard?.hide()
                            focus.clearFocus()
                        },
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                        PulseTextField(
                            set.reps,
                            {
                                onSet(index, it, null)
                            },
                            stringResource(
                                R.string.pulse_fitness_reps_set_1_s,
                                index + 1,
                            ),
                            Modifier.weight(1f).focusRequester(repsFocus[index]),
                            enabled = !busy,
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next,
                                ),
                            keyboardActions =
                                KeyboardActions(onNext = {
                                    weightFocus[index].requestFocus()
                                }),
                        )
                        Box(Modifier.weight(1f)) {
                            WeightField(
                                index,
                                set.weight,
                                busy,
                                index == draft.sets.lastIndex,
                                Modifier.focusRequester(weightFocus[index]),
                                {
                                    onSet(index, null, it)
                                },
                                { repsFocus.getOrNull(index + 1)?.requestFocus() },
                                {
                                    keyboard?.hide()
                                    focus.clearFocus()
                                },
                            )
                        }
                    }
                }
                if (draft.sets.size >
                    1
                ) {
                    PulseTextButton(
                        stringResource(
                            R.string.pulse_fitness_remove_set_1_s,
                            index + 1,
                        ),
                        { onRemoveSet(index) },
                        enabled = !busy,
                    )
                }
            }
            PulseTextButton(
                stringResource(R.string.pulse_fitness_add_another_set),
                onAddSet,
                enabled =
                    !busy && draft.sets.size < 20,
            )
            editor.error?.let {
                PulseText(
                    it,
                    Modifier.semantics {
                        liveRegion =
                            LiveRegionMode.Polite
                    },
                    color = MaterialTheme.colorScheme.error,
                )
            }
            PulseButton(
                if (draft.exerciseId ==
                    null
                ) {
                    stringResource(R.string.pulse_fitness_save_exercise)
                } else {
                    stringResource(R.string.pulse_fitness_save_changes)
                },
                {
                    keyboard?.hide()
                    onSave()
                },
                Modifier.fillMaxWidth(),
                enabled = !needsReview,
                loading = busy,
            )
            PulseTextButton(
                stringResource(R.string.pulse_fitness_cancel),
                onCancel,
                Modifier.fillMaxWidth(),
                enabled = !busy,
            )
        }
    }
}

@Composable
private fun WeightField(
    index: Int,
    value: String,
    busy: Boolean,
    last: Boolean,
    modifier: Modifier,
    onValue: (String) -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit,
) {
    PulseTextField(
        value,
        onValue,
        stringResource(
            R.string.pulse_fitness_weight_kg_set_1_s,
            index + 1,
        ),
        modifier.fillMaxWidth(),
        enabled = !busy,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = if (last) ImeAction.Done else ImeAction.Next,
            ),
        keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onDone() }),
    )
}
