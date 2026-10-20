package dev.abhishekan.pulse.feature.fitness

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.domain.model.ExerciseInput
import dev.abhishekan.pulse.core.domain.model.ExerciseSetInput
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCheckbox
import dev.abhishekan.pulse.core.ui.components.PulseIconButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

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
    onSearchQuery: (String) -> Unit,
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
        onSearchQuery(query)
        selector = true
    }
    val valid =
        draft.sets.none { it.weight.isNotBlank() && it.weight.trim().toBigDecimalOrNull() == null } &&
            ExerciseInput(
                draft.name,
                draft.sets.map {
                    ExerciseSetInput(it.reps.toIntOrNull() ?: 0, it.weight.trim().toBigDecimalOrNull())
                },
                draft.catalogId,
                draft.rememberAlias,
            ).issue == null

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulseIconButton(stringResource(R.string.editor_back), onCancel, enabled = !busy) {
                PulseText("←", style = PulseTextStyle.Title)
            }
            PulseText(
                stringResource(if (draft.exerciseId == null) R.string.editor_add_title else R.string.pulse_fitness_edit_exercise),
                Modifier.semantics { heading() },
                PulseTextStyle.Title,
            )
        }
        Crossfade(
            targetState = identityChosen || draft.name.isNotBlank() || draft.catalogId != null,
            animationSpec = tween(PulseTokens.MOTION_MILLIS),
            label = "Exercise identity",
        ) { hasName ->
            if (hasName) {
                PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
                    PulseText(stringResource(R.string.editor_selected), style = PulseTextStyle.Overline)
                    PulseText(draft.name, style = PulseTextStyle.Title)
                    selected?.let { ExerciseMetadata(it) }
                    if (draft.catalogId == null) {
                        PulseText(stringResource(R.string.editor_custom), style = PulseTextStyle.Body2)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                        WorkoutAction(stringResource(R.string.editor_change), WorkoutSymbol.Refresh, openSelector, !busy, soft = true)
                        selected?.let { EditorInstructions(it) }
                        WorkoutAction(
                            stringResource(R.string.editor_edit_name),
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
                            onName,
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
                        PulseText(stringResource(R.string.editor_matched_to, selected.name), style = PulseTextStyle.Body2)
                    }
                    if (editName && draft.catalogId != null) {
                        PulseTextButton(stringResource(R.string.editor_use_custom), onClearMatch, enabled = !busy)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .border(PulseTokens.focusWidth, PulseTheme.colors.primary, PulseTheme.shapes.medium)
                            .background(PulseTheme.colors.surface, PulseTheme.shapes.medium)
                            .clickable(enabled = !busy, onClick = openSelector)
                            .heightIn(min = PulseTokens.target)
                            .padding(PulseTokens.medium),
                        horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EditorSearchGlyph()
                        PulseText(
                            stringResource(R.string.editor_search_placeholder),
                            Modifier.weight(1f),
                            color = PulseTheme.colors.textSecondary,
                        )
                        PulseText("›", style = PulseTextStyle.Title)
                    }
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
                            PulseText(stringResource(R.string.editor_empty_title), style = PulseTextStyle.Title3)
                            PulseText(stringResource(R.string.editor_empty_body), color = PulseTheme.colors.textSecondary)
                        }
                    }
                }
            }
        }
        if (identityChosen || draft.name.isNotBlank() || draft.catalogId != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                PulseText(stringResource(R.string.pulse_fitness_your_sets), Modifier.semantics { heading() }, PulseTextStyle.Title3)
                PulseText(
                    stringResource(R.string.editor_set_count, draft.sets.size),
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
                                    { onSet(index, it, null) },
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
                                    { onSet(index, null, it) },
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
                                    "${index + 1}",
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
                                    PulseIconButton(stringResource(R.string.pulse_fitness_remove_set_1_s, index + 1), {
                                        focus.clearFocus()
                                        rowIds = rowIds.filterIndexed { i, _ -> i != index }
                                        onRemoveSet(index)
                                    }, enabled = !busy) { WorkoutGlyph(WorkoutSymbol.Delete, PulseTheme.colors.error) }
                                }
                            }
                        }
                    }
                }
            }
            WorkoutAction(stringResource(R.string.editor_add_set), WorkoutSymbol.Add, {
                rowIds = rowIds + nextRowId++
                onAddSet()
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
                            onAlias,
                            stringResource(R.string.pulse_fitness_remember_this_name_for_future_workouts),
                            enabled = !busy,
                        )
                        PulseText(
                            stringResource(R.string.editor_remember_body),
                            style = PulseTextStyle.Caption,
                            color = PulseTheme.colors.textSecondary,
                        )
                    }
                }
            }
        }
        editor.error?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = PulseTheme.colors.error) }
        PulseButton(
            stringResource(
                if (draft.exerciseId ==
                    null
                ) {
                    R.string.pulse_fitness_save_exercise
                } else {
                    R.string.pulse_fitness_save_changes
                },
            ),
            {
                keyboard?.hide()
                onSave()
            },
            Modifier.fillMaxWidth(),
            enabled = valid && !needsReview && !busy,
            loading = busy,
        )
    }
    if (selector) {
        ExerciseSelector(
            editor,
            query,
            busy,
            {
                query = it
                onSearchQuery(it)
            },
            onChoose = {
                identityChosen = true
                editName = false
                onChoose(it)
                keyboard?.hide()
                selector = false
            },
            onCustom = {
                identityChosen = true
                editName = false
                onClearMatch()
                onName(query)
                keyboard?.hide()
                selector = false
            },
            onRetry = { onSearchQuery(query) },
            onDismiss = {
                selector = false
                keyboard?.hide()
            },
        )
    }
}
