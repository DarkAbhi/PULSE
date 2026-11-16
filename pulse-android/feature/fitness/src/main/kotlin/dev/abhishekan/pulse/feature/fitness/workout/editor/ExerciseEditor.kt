package dev.abhishekan.pulse.feature.fitness.workout.editor

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCheckbox
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.fitness.R
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutAction
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutActionButton
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutGlyph
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutSymbol

@Composable
internal fun ExerciseEditor(
    editor: ExerciseEditorState,
    busy: Boolean,
    onAction: (WorkoutAction) -> Unit,
) {
    val draft = editor.draft
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var editName by rememberSaveable { mutableStateOf(false) }
    var identityChosen by rememberSaveable { mutableStateOf(draft.name.isNotBlank() || draft.catalogId != null) }
    var selector by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val selected =
        editor.selectedCatalog?.takeIf { it.id == draft.catalogId }
            ?: editor.candidates.firstOrNull { it.id == draft.catalogId }
    // Presentation identities stay attached to the surviving rows, including identical values.
    var rowIds by rememberSaveable { mutableStateOf<List<Int>>(draft.sets.indices.toList()) }
    var nextRowId by rememberSaveable { mutableIntStateOf(draft.sets.size) }
    val openSelector = {
        focus.clearFocus()
        query = draft.name
        onAction(WorkoutAction.SearchCatalogue(query))
        selector = true
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        Crossfade(
            targetState = identityChosen || draft.name.isNotBlank() || draft.catalogId != null,
            animationSpec = tween(PulseTokens.MOTION_MILLIS),
            label = "Exercise identity",
        ) { hasName ->
            if (hasName) {
                PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
                    PulseText(stringResource(R.string.pulse_fitness_editor_selected), style = PulseTextStyle.Overline)
                    PulseText(draft.name, style = PulseTextStyle.Title)
                    selected?.let { ExerciseMetadata(it) }
                    if (draft.catalogId == null) {
                        PulseText(stringResource(R.string.pulse_fitness_editor_custom), style = PulseTextStyle.Body2)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                        WorkoutActionButton(
                            stringResource(R.string.pulse_fitness_editor_change),
                            WorkoutSymbol.Refresh,
                            openSelector,
                            !busy,
                            soft = true,
                        )
                        selected?.let { ExerciseInstructions(it) }
                        WorkoutActionButton(
                            stringResource(R.string.pulse_fitness_editor_edit_name),
                            WorkoutSymbol.Edit,
                            { editName = !editName },
                            !busy,
                            soft = true,
                        )
                    }
                    // Editing an alias never clears its catalogue match.
                    if (editName) {
                        PulseTextField(
                            draft.name,
                            { onAction(WorkoutAction.ChangeName(it)) },
                            stringResource(R.string.pulse_fitness_exercise_name),
                            Modifier.fillMaxWidth(),
                            enabled = !busy,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions =
                                KeyboardActions(onDone = {
                                    focus.clearFocus()
                                    keyboard?.hide()
                                }),
                        )
                    }
                    if (selected != null && draft.name != selected.name) {
                        PulseText(stringResource(R.string.pulse_fitness_editor_matched_to, selected.name), style = PulseTextStyle.Body2)
                    }
                    if (editName && draft.catalogId != null) {
                        PulseButton(
                            stringResource(R.string.pulse_fitness_editor_use_custom),
                            { onAction(WorkoutAction.ClearMatch) },
                            enabled = !busy,
                            style = PulseButtonStyle.Text,
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                    PulseButton(
                        text = stringResource(R.string.pulse_fitness_editor_search_placeholder),
                        onClick = openSelector,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                        variant = PulseButtonVariant.Secondary,
                        leadingIcon = { EditorSearchGlyph() },
                    )
                    PulseCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = PulseTokens.large),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
                        ) {
                            Box(
                                Modifier.size(PulseTheme.spacing.spacious).background(PulseTheme.colors.primarySoft, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { WorkoutGlyph(WorkoutSymbol.Weight, PulseTheme.colors.action) }
                            PulseText(stringResource(R.string.pulse_fitness_editor_empty_title), style = PulseTextStyle.Title3)
                            PulseText(stringResource(R.string.pulse_fitness_editor_empty_body), color = PulseTheme.colors.textSecondary)
                        }
                    }
                }
            }
        }
        if (identityChosen || draft.name.isNotBlank() || draft.catalogId != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                PulseText(stringResource(R.string.pulse_fitness_your_sets), Modifier.semantics { heading() }, PulseTextStyle.Title3)
                PulseText(
                    stringResource(R.string.pulse_fitness_editor_set_count, draft.sets.size),
                    style = PulseTextStyle.Caption,
                    color = PulseTheme.colors.textSecondary,
                )
            }
            PulseCard(Modifier.fillMaxWidth().animateContentSize(animationSpec = tween(PulseTokens.MOTION_MILLIS))) {
                draft.sets.forEachIndexed { index, set ->
                    key(rowIds.getOrElse(index) { index }) {
                        val setLabel = stringResource(R.string.pulse_fitness_set_1_s, index + 1)
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val stacked = maxWidth / LocalDensity.current.fontScale < PulseTokens.formColumnsWidth / 2
                            val reps: @Composable (Modifier) -> Unit = { modifier ->
                                PulseTextField(
                                    set.reps,
                                    { onAction(WorkoutAction.ChangeSet(index, it, null)) },
                                    stringResource(R.string.pulse_fitness_reps_set_1_s, index + 1),
                                    modifier,
                                    enabled = !busy,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }),
                                )
                            }
                            val weight: @Composable (Modifier) -> Unit = { modifier ->
                                PulseTextField(
                                    set.weight,
                                    { onAction(WorkoutAction.ChangeSet(index, null, it)) },
                                    stringResource(R.string.pulse_fitness_weight_kg_set_1_s, index + 1),
                                    modifier,
                                    enabled = !busy,
                                    keyboardOptions =
                                        KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal,
                                            imeAction = if (index == draft.sets.lastIndex) ImeAction.Done else ImeAction.Next,
                                        ),
                                    keyboardActions =
                                        KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }, onDone = {
                                            keyboard?.hide()
                                            focus.clearFocus()
                                        }),
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
                            ) {
                                PulseText(
                                    (index + 1).toString(),
                                    Modifier
                                        .background(PulseTheme.colors.surfaceMuted, CircleShape)
                                        .padding(PulseTokens.small)
                                        .semantics { contentDescription = setLabel },
                                    PulseTextStyle.Label,
                                )
                                if (stacked) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                                        reps(Modifier.fillMaxWidth())
                                        weight(Modifier.fillMaxWidth())
                                    }
                                } else {
                                    reps(Modifier.weight(1f))
                                    weight(Modifier.weight(1f))
                                }
                                if (draft.sets.size > 1) {
                                    PulseButton(stringResource(R.string.pulse_fitness_remove_set_1_s, index + 1), {
                                        focus.clearFocus()
                                        rowIds = rowIds.filterIndexed { i, _ -> i != index }
                                        onAction(WorkoutAction.RemoveSet(index))
                                    }, enabled = !busy, style = PulseButtonStyle.Icon) {
                                        WorkoutGlyph(
                                            WorkoutSymbol.Delete,
                                            PulseTheme.colors.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            WorkoutActionButton(stringResource(R.string.pulse_fitness_editor_add_set), WorkoutSymbol.Add, {
                rowIds = rowIds + nextRowId++
                onAction(WorkoutAction.AddSet)
            }, !busy && draft.sets.size < 20, Modifier.fillMaxWidth().background(PulseTheme.colors.primarySoft, PulseTheme.shapes.medium))
            PulseText(
                stringResource(R.string.pulse_fitness_repetitions_1_1_000_per_set_leave_weight_blank),
                style = PulseTextStyle.Caption,
                color = PulseTheme.colors.textSecondary,
            )
        }
        if (draft.catalogId != null) {
            PulseCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                    WorkoutGlyph(WorkoutSymbol.Document, PulseTheme.colors.action)
                    Column(Modifier.weight(1f)) {
                        PulseCheckbox(
                            draft.rememberAlias,
                            { onAction(WorkoutAction.RememberAlias(it)) },
                            stringResource(R.string.pulse_fitness_remember_this_name_for_future_workouts),
                            enabled = !busy,
                        )
                        PulseText(
                            stringResource(R.string.pulse_fitness_editor_remember_body),
                            style = PulseTextStyle.Caption,
                            color = PulseTheme.colors.textSecondary,
                        )
                    }
                }
            }
        }
        editor.error?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = PulseTheme.colors.error) }
    }
    if (selector) {
        ExerciseSelector(
            editor,
            query,
            busy,
            {
                query = it
                onAction(WorkoutAction.SearchCatalogue(it))
            },
            onChoose = {
                identityChosen = true
                editName = false
                onAction(WorkoutAction.ChooseMatch(it))
                keyboard?.hide()
                selector = false
            },
            onCustom = {
                identityChosen = true
                editName = false
                onAction(WorkoutAction.ClearMatch)
                onAction(WorkoutAction.ChangeName(query))
                keyboard?.hide()
                selector = false
            },
            onRetry = { onAction(WorkoutAction.SearchCatalogue(query)) },
            onDismiss = {
                selector = false
                keyboard?.hide()
            },
        )
    }
}

@Composable
private fun EditorSearchGlyph() {
    val color = PulseTheme.colors.action
    Canvas(Modifier.size(PulseTheme.spacing.cardPadding)) {
        val u = size.width / 24
        drawCircle(color, 7 * u, Offset(10 * u, 10 * u), style = Stroke(2 * u))
        drawLine(color, Offset(15 * u, 15 * u), Offset(21 * u, 21 * u), 2 * u)
    }
}
