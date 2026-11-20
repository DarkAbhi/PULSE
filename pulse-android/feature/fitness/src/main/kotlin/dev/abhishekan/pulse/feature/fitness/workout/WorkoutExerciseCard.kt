package dev.abhishekan.pulse.feature.fitness.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.abhishekan.pulse.core.domain.model.WorkoutExercise
import dev.abhishekan.pulse.core.domain.model.WorkoutSet
import dev.abhishekan.pulse.core.ui.PulseSpacing
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.fitness.R
import dev.abhishekan.pulse.feature.fitness.workout.editor.ExerciseInstructions
import java.text.NumberFormat
import dev.abhishekan.pulse.core.ui.R as PulseUiR

@Composable
internal fun WorkoutExerciseCard(
    exercise: WorkoutExercise,
    number: Int,
    expanded: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PulseTheme.colors
    val shapes = PulseTheme.shapes
    val sets = exercise.sets
    val bodyweight = remember(sets) { sets.isNotEmpty() && sets.all { it.weight == null } }
    val expandedLabel =
        stringResource(
            if (expanded) R.string.pulse_fitness_workout_exercise_expanded else R.string.pulse_fitness_workout_exercise_collapsed,
        )
    val toggleLabel =
        stringResource(if (expanded) R.string.pulse_fitness_workout_collapse_exercise else R.string.pulse_fitness_workout_expand_exercise)
    val header = remember { BringIntoViewRequester() }
    var revealHeader by remember { mutableStateOf(false) }
    LaunchedEffect(expanded) {
        if (expanded && revealHeader) {
            header.bringIntoView()
            revealHeader = false
        }
    }

    PulseCard(modifier.fillMaxWidth(), shape = shapes.extraLarge, contentPadding = PulseSpacing.none) {
        Row(
            Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(header)
                .clickable(role = Role.Button, onClickLabel = toggleLabel) {
                    revealHeader = !expanded
                    onToggle()
                }.semantics {
                    heading()
                    stateDescription = expandedLabel
                }.animateContentSize(tween(PulseTokens.MOTION_MILLIS, easing = FastOutSlowInEasing))
                .padding(start = PulseSpacing.medium, top = PulseSpacing.medium, end = PulseSpacing.medium)
                .padding(bottom = if (expanded) PulseSpacing.tiny else PulseSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(PulseSpacing.compact),
        ) {
            Box(
                Modifier
                    .sizeIn(minWidth = PulseSpacing.extraLarge, minHeight = PulseSpacing.extraLarge)
                    .background(colors.primarySoft, shapes.medium)
                    .padding(PulseSpacing.tiny),
                contentAlignment = Alignment.Center,
            ) {
                PulseText(number.toString(), style = PulseTextStyle.Label, color = colors.onPrimarySoft)
            }
            Column(Modifier.weight(1f)) {
                PulseText(
                    exercise.name,
                    style = PulseTextStyle.Title3,
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val setCount = pluralStringResource(R.plurals.pulse_fitness_workout_set_count, sets.size, sets.size)
                PulseText(
                    if (expanded) setCount else collapsedExerciseSummary(sets, setCount, bodyweight),
                    style = PulseTextStyle.Body2,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val equipment = exercise.catalog?.equipment?.takeIf { it.isNotBlank() }
                val muscles = exercise.catalog?.muscles.orEmpty()
                val muscleLabel = remember(muscles) { muscles.filter { it.isNotBlank() }.joinToString() }
                if (bodyweight || equipment != null || muscleLabel.isNotEmpty()) {
                    FlowRow(
                        Modifier.padding(top = PulseSpacing.small),
                        horizontalArrangement = Arrangement.spacedBy(PulseSpacing.small),
                        verticalArrangement = Arrangement.spacedBy(PulseSpacing.tiny),
                    ) {
                        if (bodyweight) {
                            ExercisePill(stringResource(R.string.pulse_fitness_workout_bodyweight_equipment)) { tint ->
                                Icon(
                                    painterResource(PulseUiR.drawable.pulse_person),
                                    contentDescription = null,
                                    modifier = Modifier.size(PulseSpacing.compact),
                                    tint = tint,
                                )
                            }
                        } else if (equipment != null) {
                            ExercisePill(equipment) { tint ->
                                WorkoutGlyph(WorkoutSymbol.Weight, tint, Modifier.size(PulseSpacing.compact))
                            }
                        }
                        if (muscleLabel.isNotEmpty()) {
                            ExercisePill(muscleLabel, muscle = true) { tint ->
                                WorkoutGlyph(WorkoutSymbol.Muscle, tint, Modifier.size(PulseSpacing.compact))
                            }
                        }
                    }
                }
            }
            WorkoutGlyph(
                WorkoutSymbol.ChevronDown,
                colors.action,
                Modifier.padding(top = PulseSpacing.small).size(PulseSpacing.comfortable).rotate(if (expanded) 180f else 0f),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(PulseTokens.MOTION_MILLIS, easing = FastOutSlowInEasing), expandFrom = Alignment.Top),
            exit = shrinkVertically(tween(PulseTokens.MOTION_MILLIS, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top),
        ) {
            Column(
                Modifier.padding(start = PulseSpacing.medium, end = PulseSpacing.medium, bottom = PulseSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(PulseSpacing.compact),
            ) {
                if (sets.isNotEmpty()) {
                    val repScale = if (bodyweight) remember(sets) { sets.maxOf { it.reps }.coerceAtLeast(1) } else null
                    Column(verticalArrangement = Arrangement.spacedBy(PulseSpacing.tiny)) {
                        sets.forEach { WorkoutSetRow(it, repScale) }
                    }
                }
                WorkoutExerciseActions(exercise, enabled && expanded, expanded, onEdit, onRemove)
            }
        }
    }
}

@Composable
private fun collapsedExerciseSummary(
    sets: List<WorkoutSet>,
    setCount: String,
    bodyweight: Boolean,
): String {
    if (sets.isEmpty()) return setCount
    val (minReps, maxReps) = remember(sets) { sets.minOf { it.reps } to sets.maxOf { it.reps } }
    val reps =
        when {
            minReps == maxReps -> pluralStringResource(R.plurals.pulse_fitness_workout_reps, minReps, minReps)
            bodyweight -> {
                val locale = LocalConfiguration.current.locales[0]
                val sequence =
                    remember(sets, locale) {
                        val format = NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false }
                        sets.joinToString(" / ") { format.format(it.reps) }
                    }
                stringResource(R.string.pulse_fitness_workout_reps_sequence, sequence)
            }
            else -> stringResource(R.string.pulse_fitness_workout_reps_range, minReps, maxReps)
        }
    if (bodyweight) return "$setCount · $reps"
    val weights = remember(sets) { sets.mapNotNull { it.weight } }
    val weight =
        when {
            weights.size < sets.size -> stringResource(R.string.pulse_fitness_workout_mixed_weights)
            else -> {
                val amount =
                    remember(weights) {
                        val min = weights.min()
                        val max = weights.max()
                        val value = min.stripTrailingZeros().toPlainString()
                        if (min.compareTo(max) == 0) value else "$value–${max.stripTrailingZeros().toPlainString()}"
                    }
                stringResource(R.string.pulse_fitness_weight_kg, amount)
            }
        }
    return "$setCount · $reps · $weight"
}

@Composable
private fun ExercisePill(
    text: String,
    muscle: Boolean = false,
    icon: @Composable (Color) -> Unit,
) {
    val colors = PulseTheme.colors
    val tint = if (muscle) colors.error else colors.onPrimarySoft
    Row(
        Modifier
            .background(if (muscle) colors.errorSoft else colors.primarySoft, PulseTheme.shapes.pill)
            .padding(horizontal = PulseSpacing.small, vertical = PulseSpacing.tiny),
        horizontalArrangement = Arrangement.spacedBy(PulseSpacing.tiny),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon(tint)
        PulseText(text, style = PulseTextStyle.Label2, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun WorkoutSetRow(
    set: WorkoutSet,
    repScale: Int?,
) {
    val colors = PulseTheme.colors
    val reps = pluralStringResource(R.plurals.pulse_fitness_workout_reps, set.reps, set.reps)
    val boldValue = PulseTheme.typography.label1.copy(fontWeight = FontWeight.Bold)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = PulseSpacing.target)
            .background(lerp(colors.surface, colors.primarySoft, 0.3f), PulseTheme.shapes.large)
            .padding(horizontal = PulseSpacing.compact, vertical = PulseSpacing.small),
        horizontalArrangement = Arrangement.spacedBy(PulseSpacing.compact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .sizeIn(minWidth = PulseSpacing.large, minHeight = PulseSpacing.large)
                .background(colors.primary, PulseTheme.shapes.pill)
                .padding(PulseSpacing.tiny),
            contentAlignment = Alignment.Center,
        ) {
            PulseText(set.number.toString(), style = PulseTextStyle.Label, color = colors.onPrimary)
        }
        if (repScale != null) {
            val fraction = (set.reps.toFloat() / repScale).coerceIn(0f, 1f)
            Box(
                Modifier
                    .weight(2f)
                    .height(PulseSpacing.tiny)
                    .background(colors.primarySoft, PulseTheme.shapes.pill),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(PulseSpacing.tiny)
                        .background(colors.primary, PulseTheme.shapes.pill),
                )
            }
            PulseText(reps, Modifier.weight(1f), style = boldValue, textAlign = TextAlign.End)
        } else {
            PulseText(reps, Modifier.weight(1f), PulseTextStyle.Label)
            PulseText(
                set.weight?.let { stringResource(R.string.pulse_fitness_weight_kg, it.stripTrailingZeros().toPlainString()) }
                    ?: stringResource(R.string.pulse_fitness_bodyweight),
                Modifier.weight(1f),
                style = boldValue,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun WorkoutExerciseActions(
    exercise: WorkoutExercise,
    enabled: Boolean,
    expanded: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = PulseTheme.colors
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PulseSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        exercise.catalog?.takeIf { it.instructions.isNotEmpty() }?.let {
            ExerciseInstructions(it, Modifier.weight(1f), enabled = expanded)
        }
        PulseButton(
            stringResource(R.string.pulse_fitness_workout_edit),
            onEdit,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            variant = PulseButtonVariant.Secondary,
            leadingIcon = { WorkoutGlyph(WorkoutSymbol.Edit, LocalContentColor.current, Modifier.size(PulseSpacing.medium)) },
            contentPadding = PaddingValues(horizontal = PulseSpacing.tiny, vertical = PulseSpacing.small),
            textAlign = TextAlign.Center,
        )
        PulseButton(
            stringResource(R.string.pulse_fitness_workout_remove),
            onRemove,
            modifier = Modifier.background(if (enabled) colors.errorSoft else colors.surfaceMuted, PulseTheme.shapes.small),
            enabled = enabled,
            style = PulseButtonStyle.Icon,
            icon = { WorkoutGlyph(WorkoutSymbol.Delete, if (enabled) colors.error else colors.textSecondary) },
        )
    }
}
