package dev.abhishekan.pulse.feature.fitness.workout.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseBottomSheet
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.fitness.R
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutActionButton
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutGlyph
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutSymbol

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
                    .background(PulseTheme.colors.background, PulseTheme.shapes.pill)
                    .border(PulseTokens.focusWidth / 2, PulseTheme.colors.border, PulseTheme.shapes.pill)
                    .padding(horizontal = PulseTokens.small, vertical = PulseTokens.tiny),
                PulseTextStyle.Caption,
            )
        }
    }
}

@Composable
internal fun ExerciseInstructions(exercise: CatalogExercise) {
    if (!exercise.hasInstructionsDetails) return
    var open by rememberSaveable(exercise.id) { mutableStateOf(false) }
    WorkoutActionButton(
        stringResource(R.string.pulse_fitness_workout_view_instructions),
        WorkoutSymbol.Document,
        { open = true },
        true,
        soft = true,
    )
    if (open) ExerciseInstructionsSheet(exercise, { open = false })
}

internal val CatalogExercise.hasInstructionsDetails: Boolean
    get() = instructions.isNotEmpty() || muscles.isNotEmpty() || !equipment.isNullOrBlank()

@Composable
internal fun EditorInstructionsButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    PulseButton(
        stringResource(R.string.pulse_fitness_editor_how_to),
        onClick,
        modifier = modifier.heightIn(min = PulseTokens.target),
        enabled = enabled,
        variant = PulseButtonVariant.Secondary,
        leadingIcon = { WorkoutGlyph(WorkoutSymbol.Info, LocalContentColor.current, Modifier.size(PulseTokens.medium)) },
        contentPadding = PaddingValues(horizontal = PulseTokens.tiny, vertical = PulseTokens.small),
        textAlign = TextAlign.Center,
    )
}

@Composable
internal fun ExerciseInstructionsSheet(
    exercise: CatalogExercise,
    onDismiss: () -> Unit,
) {
    PulseBottomSheet(exercise.name, onDismiss, fullScreen = true) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(
                    rememberScrollState(),
                ).padding(horizontal = PulseTokens.medium, vertical = PulseTokens.medium),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
        ) {
            if (exercise.instructions.isNotEmpty()) {
                PulseText(
                    stringResource(R.string.pulse_fitness_editor_instructions),
                    Modifier.semantics { heading() },
                    PulseTextStyle.Label,
                )
                exercise.instructions.forEachIndexed { index, instruction ->
                    PulseText(stringResource(R.string.pulse_fitness_numbered_instruction, index + 1, instruction))
                }
            }
            if (exercise.muscles.isNotEmpty()) {
                PulseText(
                    stringResource(R.string.pulse_fitness_editor_target_muscles),
                    Modifier.semantics { heading() },
                    PulseTextStyle.Label,
                )
                PulseText(exercise.muscles.joinToString())
            }
            exercise.equipment?.takeIf { it.isNotBlank() }?.let {
                PulseText(stringResource(R.string.pulse_fitness_workout_equipment), Modifier.semantics { heading() }, PulseTextStyle.Label)
                PulseText(it)
            }
        }
    }
}
