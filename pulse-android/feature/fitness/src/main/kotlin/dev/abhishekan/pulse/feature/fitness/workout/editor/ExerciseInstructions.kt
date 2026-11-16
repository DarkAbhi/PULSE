package dev.abhishekan.pulse.feature.fitness.workout.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseBottomSheet
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.fitness.R
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutActionButton
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
                    .background(PulseTheme.colors.surface, PulseTheme.shapes.pill)
                    .border(PulseTokens.focusWidth / 2, PulseTheme.colors.border, PulseTheme.shapes.pill)
                    .padding(horizontal = PulseTokens.small, vertical = PulseTokens.tiny),
                PulseTextStyle.Caption,
            )
        }
    }
}

@Composable
internal fun ExerciseInstructions(exercise: CatalogExercise) {
    var open by rememberSaveable(exercise.id) { mutableStateOf(false) }
    if (exercise.instructions.isEmpty() && exercise.muscles.isEmpty() && exercise.equipment.isNullOrBlank()) return
    WorkoutActionButton(
        stringResource(R.string.pulse_fitness_workout_view_instructions),
        WorkoutSymbol.Document,
        { open = true },
        true,
        soft = true,
    )
    if (open) ExerciseInstructionsSheet(exercise, { open = false })
}

@Composable
private fun ExerciseInstructionsSheet(
    exercise: CatalogExercise,
    onDismiss: () -> Unit,
) {
    PulseBottomSheet(exercise.name, onDismiss, expandOnOverflow = true) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = PulseTokens.medium, vertical = PulseTokens.medium),
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
