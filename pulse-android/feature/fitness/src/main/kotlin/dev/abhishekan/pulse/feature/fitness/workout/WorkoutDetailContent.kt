package dev.abhishekan.pulse.feature.fitness.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.WorkoutDetail
import dev.abhishekan.pulse.core.domain.model.WorkoutExercise
import dev.abhishekan.pulse.core.ui.PulseSpacing
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.fitness.R
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.ZoneId

internal fun LazyGridScope.workoutDetailItems(
    detail: WorkoutDetail,
    timeZone: ZoneId,
    enabled: Boolean,
    onEditWorkout: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onDelete: () -> Unit,
) {
    item(key = "workout-information", span = { GridItemSpan(maxLineSpan) }) {
        WorkoutHero(detail.info, timeZone, enabled, onEditWorkout)
    }
    item(key = "workout-exercises-heading", span = { GridItemSpan(maxLineSpan) }) {
        WorkoutExercisesHeading(detail.exercises, enabled, onAdd)
    }
    if (detail.exercises.isEmpty()) {
        item(key = "workout-empty", span = { GridItemSpan(maxLineSpan) }) {
            EmptyWorkoutExercises()
        }
    }
    items(detail.exercises, key = { it.id }, contentType = { "exercise" }) { exercise ->
        WorkoutExerciseCard(exercise, enabled, { onEdit(exercise.id) }, { onRemove(exercise.id) })
    }
    item(key = "workout-management", span = { GridItemSpan(maxLineSpan) }) {
        WorkoutManagement(enabled, onDelete)
    }
}

@Composable
private fun WorkoutExercisesHeading(
    exercises: List<WorkoutExercise>,
    enabled: Boolean,
    onAdd: () -> Unit,
) {
    val subtitle =
        if (exercises.isEmpty()) {
            stringResource(R.string.pulse_fitness_workout_exercises_empty)
        } else {
            val (sets, volume) =
                remember(exercises) {
                    val sets = exercises.flatMap { it.sets }
                    val volume = sets.sumOf { (it.weight ?: BigDecimal.ZERO).multiply(it.reps.toBigDecimal()) }
                    sets.size to volume
                }
            val exerciseLabel = pluralStringResource(R.plurals.pulse_fitness_workout_exercise_count, exercises.size, exercises.size)
            val setLabel = pluralStringResource(R.plurals.pulse_fitness_workout_set_count, sets, sets)
            val volumeLabel =
                volume.takeIf { it.signum() > 0 }?.let {
                    stringResource(R.string.pulse_fitness_workout_volume, formatWorkoutNumber(it))
                }
            listOfNotNull(exerciseLabel, setLabel, volumeLabel).joinToString(" · ")
        }
    Column(verticalArrangement = Arrangement.spacedBy(PulseSpacing.medium)) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(PulseSpacing.small),
        ) {
            Column(Modifier.align(Alignment.CenterVertically).padding(end = PulseSpacing.small)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PulseSpacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WorkoutGlyph(WorkoutSymbol.List, PulseTheme.colors.brand)
                    PulseText(
                        stringResource(R.string.pulse_fitness_workout_exercises),
                        Modifier.semantics { heading() },
                        PulseTextStyle.Heading3,
                    )
                }
                PulseText(subtitle, style = PulseTextStyle.Body2, color = PulseTheme.colors.textSecondary)
            }
            PulseButton(
                stringResource(R.string.pulse_fitness_add_exercise),
                onAdd,
                Modifier.align(Alignment.CenterVertically),
                enabled = enabled,
                leadingIcon = { WorkoutGlyph(WorkoutSymbol.Add, LocalContentColor.current, Modifier.size(18.dp)) },
            )
        }
        HorizontalDivider(color = PulseTheme.colors.border)
    }
}

@Composable
private fun EmptyWorkoutExercises() {
    val border = PulseTheme.colors.border
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 1.5.dp.toPx()
            drawRoundRect(
                color = border,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                cornerRadius = CornerRadius(24.dp.toPx()),
                style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
            )
        }
        PulseText(
            stringResource(R.string.pulse_fitness_workout_empty),
            Modifier.fillMaxWidth().padding(horizontal = PulseSpacing.comfortable, vertical = 36.dp),
            style = PulseTextStyle.Body2,
            color = PulseTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WorkoutManagement(
    enabled: Boolean,
    onDelete: () -> Unit,
) {
    PulseCard(Modifier.fillMaxWidth()) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(PulseSpacing.small),
        ) {
            Column(Modifier.align(Alignment.CenterVertically).padding(end = PulseSpacing.small)) {
                PulseText(
                    stringResource(R.string.pulse_fitness_delete_workout),
                    Modifier.semantics { heading() },
                    PulseTextStyle.Title3,
                    color = PulseTheme.colors.error,
                )
                PulseText(
                    stringResource(R.string.pulse_fitness_workout_manage_description),
                    style = PulseTextStyle.Body2,
                    color = PulseTheme.colors.textSecondary,
                )
            }
            WorkoutActionButton(
                stringResource(R.string.pulse_fitness_delete_workout),
                WorkoutSymbol.Delete,
                onDelete,
                enabled,
                Modifier.align(Alignment.CenterVertically),
                destructive = true,
            )
        }
    }
}

@Composable
internal fun formatWorkoutNumber(
    value: BigDecimal,
    fractionDigits: Int = 2,
): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(value, locale, fractionDigits) {
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = fractionDigits }.format(value)
    }
}
