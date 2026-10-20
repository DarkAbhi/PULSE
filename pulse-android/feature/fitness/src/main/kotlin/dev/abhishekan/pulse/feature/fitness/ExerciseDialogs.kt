package dev.abhishekan.pulse.feature.fitness

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseIconButton
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

/** Shared bounded modal: centered on wide windows, nearly full screen on compact ones. */
@Composable
internal fun ExerciseModal(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.Center) {
            val compact = maxWidth < PulseTokens.readingWidth
            Column(
                Modifier
                    .padding(if (compact) PulseTokens.small else PulseTokens.cardPadding)
                    .widthIn(max = PulseTokens.readingWidth)
                    .fillMaxWidth()
                    .then(if (compact) Modifier.fillMaxHeight() else Modifier.heightIn(max = maxHeight * .9f))
                    .background(PulseTheme.colors.surface, PulseTheme.shapes.extraLarge)
                    .border(PulseTokens.focusWidth / 2, PulseTheme.colors.border, PulseTheme.shapes.extraLarge)
                    .padding(PulseTokens.medium),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PulseText(title, Modifier.weight(1f).semantics { heading() }, PulseTextStyle.Title3)
                    PulseTextButton(stringResource(R.string.workout_close), onDismiss)
                }
                content()
            }
        }
    }
}

@Composable
internal fun ExerciseSelector(
    editor: ExerciseEditorState,
    query: String,
    busy: Boolean,
    onQuery: (String) -> Unit,
    onChoose: (CatalogExercise) -> Unit,
    onCustom: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    ExerciseModal(stringResource(R.string.editor_search_title), onDismiss) {
        PulseTextField(
            query,
            onQuery,
            stringResource(R.string.editor_search_placeholder),
            Modifier.fillMaxWidth(),
            enabled = !busy,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions =
                KeyboardActions(onSearch = {
                    onRetry()
                    keyboard?.hide()
                    focus.clearFocus()
                }),
            trailingIcon = {
                if (query.isNotEmpty()) {
                    PulseIconButton(stringResource(R.string.editor_clear_search), { onQuery("") }) {
                        PulseText("×")
                    }
                }
            },
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .semantics { selectableGroup() },
            verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
        ) {
            if (editor.searching) PulseLoadingState(stringResource(R.string.pulse_fitness_finding_exercises))
            editor.searchError?.let {
                PulseText(it, color = PulseTheme.colors.error)
                PulseTextButton(stringResource(R.string.pulse_fitness_retry_catalogue_search), onRetry, enabled = !busy)
            }
            if (!editor.searching && editor.searchError == null && editor.candidates.isEmpty()) {
                PulseText(
                    stringResource(
                        if (query.isBlank()) {
                            R.string.editor_search_hint
                        } else {
                            R.string.pulse_fitness_no_matches_yet_you_can_save_your_own_exercise
                        },
                    ),
                    Modifier.padding(vertical = PulseTokens.medium),
                    color = PulseTheme.colors.textSecondary,
                )
            }
            editor.candidates.forEach { candidate ->
                val selected = editor.draft.catalogId == candidate.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (selected) PulseTheme.colors.primarySoft else PulseTheme.colors.surface, PulseTheme.shapes.medium)
                        .border(
                            PulseTokens.focusWidth / 2,
                            if (selected) PulseTheme.colors.primary else PulseTheme.colors.border,
                            PulseTheme.shapes.medium,
                        ).clickable(enabled = !busy, role = androidx.compose.ui.semantics.Role.RadioButton) { onChoose(candidate) }
                        .semantics { this.selected = selected }
                        .heightIn(min = PulseTokens.target)
                        .padding(PulseTokens.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny)) {
                        PulseText(candidate.name, style = PulseTextStyle.Label)
                        val metadata =
                            listOfNotNull(
                                candidate.equipment?.takeIf { it.isNotBlank() },
                                candidate.muscles.takeIf { it.isNotEmpty() }?.joinToString(),
                            )
                        if (metadata.isNotEmpty()) {
                            PulseText(
                                metadata.joinToString(" · "),
                                style = PulseTextStyle.Body2,
                                color = PulseTheme.colors.textSecondary,
                            )
                        }
                    }
                    PulseText(if (selected) "✓" else "+", style = PulseTextStyle.Title, color = PulseTheme.colors.action)
                }
            }
        }
        if (query.isNotBlank()) {
            PulseTextButton(stringResource(R.string.editor_custom_action, query), onCustom, Modifier.fillMaxWidth(), enabled = !busy)
        }
    }
}

@Composable
internal fun ExerciseMetadata(exercise: CatalogExercise) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
        verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
    ) {
        (listOfNotNull(exercise.equipment?.takeIf { it.isNotBlank() }) + exercise.muscles).forEach { text ->
            PulseText(
                text,
                Modifier
                    .background(PulseTheme.colors.surface, PulseTheme.shapes.pill)
                    .border(PulseTokens.focusWidth / 2, PulseTheme.colors.border, PulseTheme.shapes.pill)
                    .padding(horizontal = PulseTokens.small, vertical = PulseTokens.tiny),
                PulseTextStyle.Caption,
            )
        }
    }
}

@Composable
internal fun EditorInstructions(exercise: CatalogExercise) {
    var open by rememberSaveable(exercise.id) { mutableStateOf(false) }
    if (exercise.instructions.isEmpty() && exercise.muscles.isEmpty() && exercise.equipment.isNullOrBlank()) return
    WorkoutAction(stringResource(R.string.workout_view_instructions), WorkoutSymbol.Document, { open = true }, true, soft = true)
    if (open) ExerciseInstructionsDialog(exercise, { open = false })
}

@Composable
internal fun ExerciseInstructionsDialog(
    exercise: CatalogExercise,
    onDismiss: () -> Unit,
) {
    ExerciseModal(exercise.name, onDismiss) {
        Column(
            Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
        ) {
            if (exercise.instructions.isNotEmpty()) {
                PulseText(stringResource(R.string.editor_instructions), Modifier.semantics { heading() }, PulseTextStyle.Label)
                exercise.instructions.forEachIndexed { index, instruction ->
                    PulseText(stringResource(R.string.pulse_fitness_numbered_instruction, index + 1, instruction))
                }
            }
            if (exercise.muscles.isNotEmpty()) {
                PulseText(stringResource(R.string.editor_target_muscles), Modifier.semantics { heading() }, PulseTextStyle.Label)
                PulseText(exercise.muscles.joinToString())
            }
            exercise.equipment?.takeIf { it.isNotBlank() }?.let {
                PulseText(stringResource(R.string.workout_equipment), Modifier.semantics { heading() }, PulseTextStyle.Label)
                PulseText(it)
            }
        }
    }
}

@Composable
internal fun EditorSearchGlyph() {
    val color = PulseTheme.colors.action
    Canvas(Modifier.size(PulseTheme.spacing.cardPadding)) {
        val u = size.width / 24
        drawCircle(color, 7 * u, Offset(10 * u, 10 * u), style = Stroke(2 * u))
        drawLine(color, Offset(15 * u, 15 * u), Offset(21 * u, 21 * u), 2 * u)
    }
}
