package dev.abhishekan.pulse.feature.fitness.workout.editor

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCheckbox
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.fitness.R
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutAction
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutGlyph
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutSymbol

private enum class ExerciseEditorSheet {
    Selector,
    Instructions,
}

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
    var sheet by rememberSaveable { mutableStateOf<ExerciseEditorSheet?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val selected =
        editor.selectedCatalog?.takeIf { it.id == draft.catalogId }
            ?: editor.candidates.firstOrNull { it.id == draft.catalogId }
    val instructions = selected?.takeIf { it.hasInstructionsDetails }
    val openSelector = {
        focus.clearFocus()
        query = draft.name
        onAction(WorkoutAction.SearchCatalogue(query))
        sheet = ExerciseEditorSheet.Selector
    }
    val openInstructions = { sheet = ExerciseEditorSheet.Instructions }

    val hasExercise = identityChosen || draft.name.isNotBlank() || draft.catalogId != null

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        Crossfade(
            targetState = hasExercise,
            animationSpec = tween(PulseTokens.MOTION_MILLIS),
            label = "Exercise identity",
        ) { hasName ->
            if (hasName) {
                PulseCard(
                    Modifier.fillMaxWidth(),
                    shape = PulseTheme.shapes.extraLarge,
                    contentPadding = PulseTokens.medium,
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny)) {
                            PulseText(
                                stringResource(R.string.pulse_fitness_editor_selected),
                                style = PulseTextStyle.Overline,
                                color = PulseTheme.colors.action,
                            )
                            PulseText(draft.name, style = PulseTextStyle.Heading3, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                        PulseButton(
                            stringResource(R.string.pulse_fitness_editor_edit_name),
                            { editName = !editName },
                            modifier = Modifier.clip(PulseTheme.shapes.medium).background(PulseTheme.colors.surfaceMuted),
                            enabled = !busy,
                            style = PulseButtonStyle.Icon,
                        ) {
                            WorkoutGlyph(WorkoutSymbol.Edit, if (busy) PulseTheme.colors.textSecondary else PulseTheme.colors.action)
                        }
                    }
                    selected?.let { ExerciseMetadata(it) }
                    if (draft.catalogId == null) {
                        PulseText(stringResource(R.string.pulse_fitness_editor_custom), style = PulseTextStyle.Body2)
                    }
                    if (selected != null && draft.name != selected.name) {
                        PulseText(
                            stringResource(R.string.pulse_fitness_editor_matched_to, selected.name),
                            style = PulseTextStyle.Body2,
                            color = PulseTheme.colors.textSecondary,
                        )
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val stacked = maxWidth / LocalDensity.current.fontScale < PulseTokens.formColumnsWidth / 2
                        val change: @Composable (Modifier) -> Unit = { modifier ->
                            PulseButton(
                                stringResource(R.string.pulse_fitness_editor_change),
                                openSelector,
                                modifier = modifier.heightIn(min = PulseTokens.target),
                                enabled = !busy,
                                variant = PulseButtonVariant.Secondary,
                                leadingIcon = {
                                    WorkoutGlyph(WorkoutSymbol.Refresh, LocalContentColor.current, Modifier.size(PulseTokens.medium))
                                },
                                contentPadding = PaddingValues(horizontal = PulseTokens.tiny, vertical = PulseTokens.small),
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (stacked || instructions == null) {
                            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                                change(Modifier.fillMaxWidth())
                                if (instructions != null) EditorInstructionsButton(openInstructions, !busy, Modifier.fillMaxWidth())
                            }
                        } else {
                            Row(
                                Modifier.height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
                            ) {
                                change(Modifier.weight(1f).fillMaxHeight())
                                EditorInstructionsButton(openInstructions, !busy, Modifier.weight(1f).fillMaxHeight())
                            }
                        }
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
                        if (draft.catalogId != null) {
                            PulseButton(
                                stringResource(R.string.pulse_fitness_editor_use_custom),
                                { onAction(WorkoutAction.ClearMatch) },
                                enabled = !busy,
                                style = PulseButtonStyle.Text,
                            )
                        }
                    }
                    if (draft.catalogId != null) {
                        PulseHorizontalDivider()
                        Column {
                            PulseCheckbox(
                                draft.rememberAlias,
                                { onAction(WorkoutAction.RememberAlias(it)) },
                                stringResource(R.string.pulse_fitness_remember_this_name_for_future_workouts),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !busy,
                            )
                            PulseText(
                                stringResource(R.string.pulse_fitness_editor_remember_body),
                                modifier = Modifier.padding(start = PulseTokens.large),
                                style = PulseTextStyle.Caption,
                                color = PulseTheme.colors.textSecondary,
                            )
                        }
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
        if (hasExercise) ExerciseSetEditor(draft.sets, busy, onAction)
        editor.error?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = PulseTheme.colors.error) }
    }
    when (sheet) {
        ExerciseEditorSheet.Selector ->
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
                    sheet = null
                },
                onCustom = {
                    identityChosen = true
                    editName = false
                    onAction(WorkoutAction.ClearMatch)
                    onAction(WorkoutAction.ChangeName(query))
                    keyboard?.hide()
                    sheet = null
                },
                onRetry = { onAction(WorkoutAction.SearchCatalogue(query)) },
                onDismiss = {
                    sheet = null
                    keyboard?.hide()
                },
            )
        ExerciseEditorSheet.Instructions ->
            instructions?.let { ExerciseInstructionsSheet(it, onDismiss = { sheet = null }) }
        null -> Unit
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
