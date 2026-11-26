package dev.abhishekan.pulse.feature.fitness.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.WorkoutExercise
import dev.abhishekan.pulse.core.ui.PulseSpacing
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.fitness.R
import dev.abhishekan.pulse.feature.fitness.workout.editor.ExerciseInstructions

@Composable
internal fun WorkoutExerciseCard(
    exercise: WorkoutExercise,
    enabled: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = PulseTheme.colors
    val catalog = exercise.catalog
    val muscles = catalog?.muscles.orEmpty().filter(String::isNotBlank)
    val muscleColor = exerciseMuscleColor(muscles.firstOrNull(), colors.brand)
    PulseCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(PulseSpacing.medium)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(PulseSpacing.compact),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(48.dp).background(muscleColor, PulseTheme.shapes.large),
                    contentAlignment = Alignment.Center,
                ) {
                    WorkoutGlyph(WorkoutSymbol.Weight, Color.White)
                }
                Column(Modifier.weight(1f)) {
                    PulseText(exercise.name, Modifier.semantics { heading() }, PulseTextStyle.Title2)
                    catalog?.name?.takeIf { it != exercise.name }?.let {
                        PulseText(it, style = PulseTextStyle.Body2, color = colors.textSecondary)
                    }
                }
            }
            val weightType =
                when {
                    exercise.sets.isNotEmpty() && exercise.sets.all { it.weight == null } ->
                        stringResource(R.string.pulse_fitness_workout_body_weight)
                    catalog?.equipment == "body only" -> stringResource(R.string.pulse_fitness_workout_weighted)
                    else -> catalog?.equipment
                }
            ExerciseFacts(
                listOf(
                    ExerciseFact(R.string.pulse_fitness_workout_category, catalog?.category),
                    ExerciseFact(R.string.pulse_fitness_workout_weight_type, weightType),
                    ExerciseFact(R.string.pulse_fitness_workout_body_part, muscles.joinToString(), muscleColor),
                ),
            )
            ExerciseSets(exercise)
            Column(verticalArrangement = Arrangement.spacedBy(PulseSpacing.small)) {
                HorizontalDivider(color = colors.border)
                val hasInstructions = catalog?.instructions?.isNotEmpty() == true
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(PulseSpacing.small),
                ) {
                    catalog?.takeIf { hasInstructions }?.let {
                        ExerciseInstructions(it, Modifier.align(Alignment.CenterVertically), enabled = true)
                    }
                    Row(
                        Modifier
                            .align(
                                Alignment.CenterVertically,
                            ).then(if (hasInstructions) Modifier.widthIn(min = 96.dp) else Modifier.fillMaxWidth()),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        PulseButton(
                            stringResource(R.string.pulse_fitness_workout_edit),
                            onEdit,
                            enabled = enabled,
                            style = PulseButtonStyle.Icon,
                            icon = { WorkoutGlyph(WorkoutSymbol.Edit, if (enabled) colors.action else colors.textSecondary) },
                        )
                        PulseButton(
                            stringResource(R.string.pulse_fitness_workout_remove),
                            onRemove,
                            enabled = enabled,
                            style = PulseButtonStyle.Icon,
                            icon = { WorkoutGlyph(WorkoutSymbol.Delete, if (enabled) colors.error else colors.textSecondary) },
                        )
                    }
                }
            }
        }
    }
}

private data class ExerciseFact(
    val label: Int,
    val value: String?,
    val dot: Color? = null,
)

@Composable
private fun ExerciseFacts(facts: List<ExerciseFact>) {
    val colors = PulseTheme.colors
    BoxWithConstraints(Modifier.fillMaxWidth().clip(PulseTheme.shapes.large).background(colors.surfaceMuted)) {
        if (maxWidth >= 360.dp) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                facts.forEachIndexed { index, fact ->
                    if (index > 0) VerticalDivider(color = colors.border)
                    ExerciseFactCell(fact, Modifier.weight(1f))
                }
            }
        } else {
            Column(Modifier.fillMaxWidth()) {
                facts.forEachIndexed { index, fact ->
                    if (index > 0) HorizontalDivider(color = colors.border)
                    ExerciseFactCell(fact, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun ExerciseFactCell(
    fact: ExerciseFact,
    modifier: Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val value = fact.value?.takeIf { it.isNotBlank() }
    val label = value?.split(' ')?.joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(locale) } }
    Column(
        modifier.padding(horizontal = PulseSpacing.compact, vertical = PulseSpacing.small),
        verticalArrangement = Arrangement.spacedBy(PulseSpacing.tiny),
    ) {
        WorkoutColumnLabel(fact.label)
        Row(
            horizontalArrangement = Arrangement.spacedBy(PulseSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (value != null && fact.dot != null) {
                Box(Modifier.size(8.dp).background(fact.dot, PulseTheme.shapes.pill))
            }
            PulseText(label ?: stringResource(R.string.pulse_fitness_workout_missing_value), style = PulseTextStyle.Label)
        }
    }
}

@Composable
private fun ExerciseSets(exercise: WorkoutExercise) {
    val sets = exercise.sets
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PulseText(stringResource(R.string.pulse_fitness_workout_sets), style = PulseTextStyle.Label)
            WorkoutChip(pluralStringResource(R.plurals.pulse_fitness_workout_set_count, sets.size, sets.size))
        }
        SetTableRow {
            WorkoutColumnLabel(R.string.pulse_fitness_workout_set, Modifier.width(44.dp))
            WorkoutColumnLabel(R.string.pulse_fitness_workout_reps_label, Modifier.weight(1f))
            WorkoutColumnLabel(R.string.pulse_fitness_workout_weight, Modifier.weight(1f), TextAlign.End)
        }
        sets.forEach { set ->
            HorizontalDivider(color = PulseTheme.colors.border)
            SetTableRow {
                Box(Modifier.width(44.dp)) { WorkoutChip(set.number.toString()) }
                PulseText(
                    pluralStringResource(R.plurals.pulse_fitness_workout_reps, set.reps, set.reps),
                    Modifier.weight(1f),
                    PulseTextStyle.Label,
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    val weight = set.weight
                    if (weight == null) {
                        WorkoutChip(stringResource(R.string.pulse_fitness_workout_body_weight))
                    } else {
                        PulseText(
                            stringResource(R.string.pulse_fitness_weight_kg, formatWorkoutNumber(weight, fractionDigits = 3)),
                            style = PulseTextStyle.Label,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetTableRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(vertical = PulseSpacing.tiny),
        horizontalArrangement = Arrangement.spacedBy(PulseSpacing.tiny),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun WorkoutChip(text: String) {
    PulseText(
        text,
        Modifier
            .sizeIn(minWidth = 32.dp)
            .background(PulseTheme.colors.surfaceMuted, PulseTheme.shapes.pill)
            .padding(horizontal = PulseSpacing.small, vertical = PulseSpacing.tiny),
        style = PulseTextStyle.Label2,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun WorkoutColumnLabel(
    label: Int,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    PulseText(
        stringResource(label).uppercase(LocalConfiguration.current.locales[0]),
        modifier,
        PulseTextStyle.Caption,
        color = PulseTheme.colors.textSecondary,
        textAlign = textAlign,
    )
}

private fun exerciseMuscleColor(
    muscle: String?,
    fallback: Color,
): Color =
    when (muscle?.lowercase()) {
        "shoulders" -> Color(0xFFF0603A)
        "chest" -> Color(0xFFD91CAB)
        "back", "lats", "middle back", "lower back" -> Color(0xFF0F86C9)
        "biceps" -> Color(0xFF7B2CE0)
        "triceps" -> Color(0xFF5B2CE0)
        "legs", "quadriceps", "hamstrings", "calves", "glutes" -> Color(0xFF12A89A)
        "abdominals", "core" -> Color(0xFFD9930A)
        else -> fallback
    }
