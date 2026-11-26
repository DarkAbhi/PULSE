package dev.abhishekan.pulse.feature.fitness.workout.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
internal fun ExerciseInstructions(
    exercise: CatalogExercise,
    modifier: Modifier,
    weightType: String?,
) {
    var open by rememberSaveable(exercise.id) { mutableStateOf(false) }
    PulseButton(
        stringResource(R.string.pulse_fitness_editor_instructions),
        onClick = { open = true },
        modifier = modifier,
        variant = PulseButtonVariant.Soft,
        leadingIcon = { WorkoutGlyph(WorkoutSymbol.Document, LocalContentColor.current, Modifier.size(PulseTokens.medium)) },
        contentPadding = PaddingValues(horizontal = PulseTokens.tiny, vertical = PulseTokens.small),
        textAlign = TextAlign.Center,
    )
    if (open) ExerciseInstructionsSheet(exercise, onDismiss = { open = false }, weightType = weightType)
}

internal val CatalogExercise.hasInstructionsDetails: Boolean
    get() = instructions.isNotEmpty() || muscles.isNotEmpty() || !equipment.isNullOrBlank() || !category.isNullOrBlank()

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
    weightType: String? = exercise.equipment,
) {
    val metadata =
        listOfNotNull(exercise.category, weightType, exercise.muscles.filter(String::isNotBlank).joinToString())
            .filter(String::isNotBlank)
    val locale = LocalConfiguration.current.locales[0]
    PulseBottomSheet(
        title = exercise.name,
        onDismissRequest = onDismiss,
        fitContent = true,
        titleTextAlign = TextAlign.Start,
        titleStyle = PulseTextStyle.Heading3,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = PulseTokens.medium, end = PulseTokens.medium, bottom = PulseTokens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
        ) {
            if (metadata.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
                    verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
                ) {
                    metadata.forEach { value ->
                        PulseText(
                            value.split(' ').joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(locale) } },
                            Modifier
                                .background(PulseTheme.colors.chip, PulseTheme.shapes.pill)
                                .padding(horizontal = PulseTokens.small, vertical = PulseTokens.tiny),
                            PulseTextStyle.Label2,
                        )
                    }
                }
            }
            if (exercise.instructions.isEmpty()) {
                PulseText(
                    stringResource(R.string.pulse_fitness_no_exercise_instructions),
                    color = PulseTheme.colors.textSecondary,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                    exercise.instructions.forEachIndexed { index, step ->
                        ExerciseInstructionStep(index + 1, step)
                    }
                }
            }
            PulseButton(
                stringResource(R.string.pulse_fitness_instructions_got_it),
                onDismiss,
                Modifier.fillMaxWidth(),
            )
        }
    }
}

private val instructionStepGradient = Brush.linearGradient(listOf(Color(0xFF3A2CD9), Color(0xFFB22BD8)))

@Composable
private fun ExerciseInstructionStep(
    number: Int,
    text: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.compact),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .sizeIn(minWidth = PulseTokens.large, minHeight = PulseTokens.large)
                .background(instructionStepGradient, PulseTheme.shapes.pill)
                .padding(PulseTokens.tiny),
            contentAlignment = Alignment.Center,
        ) {
            PulseText(
                number.toString(),
                style = PulseTheme.typography.label1.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
        }
        PulseText(text, Modifier.weight(1f).padding(top = PulseTokens.tiny))
    }
}
