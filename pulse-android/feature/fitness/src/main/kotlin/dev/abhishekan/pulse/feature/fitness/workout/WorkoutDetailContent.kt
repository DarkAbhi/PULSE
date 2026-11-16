package dev.abhishekan.pulse.feature.fitness.workout

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.WorkoutDetail
import dev.abhishekan.pulse.core.domain.model.WorkoutExercise
import dev.abhishekan.pulse.core.domain.model.WorkoutInfo
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.fitness.R
import dev.abhishekan.pulse.feature.fitness.workout.editor.ExerciseInstructions
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun WorkoutDetailContent(
    detail: WorkoutDetail,
    timeZone: ZoneId,
    enabled: Boolean,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onDelete: () -> Unit,
) {
    WorkoutInformation(detail.info, timeZone)
    if (detail.exercises.isEmpty()) {
        PulseCard(Modifier.fillMaxWidth()) {
            WorkoutSectionTitle(stringResource(R.string.pulse_fitness_workout_exercises))
            BoxWithConstraints {
                val wide = maxWidth >= 640.dp
                val copy: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PulseText(stringResource(R.string.pulse_fitness_workout_empty), style = PulseTextStyle.Heading2)
                        PulseText(
                            stringResource(R.string.pulse_fitness_choose_a_catalogue_exercise_or_record_your_own_name),
                            color = PulseTheme.colors.textSecondary,
                        )
                        WorkoutActionButton(
                            stringResource(R.string.pulse_fitness_add_exercise),
                            WorkoutSymbol.Add,
                            onAdd,
                            enabled,
                            primary = true,
                        )
                    }
                }
                if (wide) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).padding(vertical = 24.dp)) { copy() }
                        WorkoutIllustration(R.drawable.workout_empty, Modifier.width(260.dp).height(220.dp))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        WorkoutIllustration(R.drawable.workout_empty, Modifier.fillMaxWidth().height(160.dp))
                        copy()
                    }
                }
            }
        }
    } else {
        WorkoutActionButton(
            stringResource(R.string.pulse_fitness_add_exercise),
            WorkoutSymbol.Add,
            onAdd,
            enabled,
            Modifier.fillMaxWidth(),
            primary = true,
        )
        BoxWithConstraints {
            if (maxWidth >= 840.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top) {
                    ExerciseList(detail.exercises, enabled, onEdit, onRemove, Modifier.weight(1f))
                    WorkoutSummary(detail, Modifier.width(264.dp))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    WorkoutSummary(detail, Modifier.fillMaxWidth())
                    ExerciseList(detail.exercises, enabled, onEdit, onRemove, Modifier.fillMaxWidth())
                }
            }
        }
    }
    PulseCard(Modifier.fillMaxWidth()) {
        BoxWithConstraints {
            val wide = maxWidth >= 880.dp
            val copy: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WorkoutSectionTitle(stringResource(R.string.pulse_fitness_workout_manage))
                    PulseText(
                        stringResource(R.string.pulse_fitness_workout_manage_description),
                        color = PulseTheme.colors.textSecondary,
                        style = PulseTextStyle.Body2,
                    )
                }
            }
            val actions: @Composable () -> Unit = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WorkoutActionButton(
                        stringResource(R.string.pulse_fitness_delete_workout),
                        WorkoutSymbol.Delete,
                        onDelete,
                        enabled,
                        destructive = true,
                    )
                }
            }
            if (wide) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(Modifier.weight(1f)) { copy() }
                    actions()
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    copy()
                    actions()
                }
            }
        }
    }
}

@Composable
private fun WorkoutInformation(info: WorkoutInfo, timeZone: ZoneId) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale, timeZone) { DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale).withZone(timeZone) }
    val timeFormatter = remember(locale, timeZone) { DateTimeFormatter.ofPattern("d MMM · HH:mm", locale).withZone(timeZone) }
    val hasMetrics = info.start != null || info.end != null || info.durationSeconds != null || info.calories != null
    if (!hasMetrics && !info.imported) {
        Surface(shape = RoundedCornerShape(16.dp), color = lerp(PulseTheme.colors.surface, PulseTheme.colors.primarySoft, .75f)) {
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WorkoutGlyph(WorkoutSymbol.Info, PulseTheme.colors.textSecondary)
                PulseText(
                    stringResource(R.string.pulse_fitness_timing_and_energy_data_aren_t_available_for_this),
                    style = PulseTextStyle.Body2,
                )
            }
        }
        return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        lerp(PulseTheme.colors.surface, PulseTheme.colors.primarySoft, .7f),
                        lerp(PulseTheme.colors.surface, PulseTheme.colors.primarySoft, .4f),
                    ),
                ),
                RoundedCornerShape(24.dp),
            ).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WorkoutSectionTitle(stringResource(R.string.pulse_fitness_your_workout))
        PulseText(
            dateFormatter.format(info.createdAt),
            Modifier.semantics {
                heading()
            },
            PulseTextStyle.Heading2,
        )
        if (!info.imported) {
            PulseText(stringResource(R.string.pulse_fitness_recorded_gym_visit))
        }
        if (hasMetrics) {
            BoxWithConstraints(Modifier.padding(vertical = 8.dp)) {
                val columns = if (maxWidth >= 720.dp) 4 else 2
                val tileWidth = (maxWidth - 12.dp * (columns - 1) - 1.dp) / columns
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    info.start?.let {
                        MetricTile(
                            stringResource(R.string.pulse_fitness_workout_started),
                            timeFormatter.format(it),
                            WorkoutSymbol.Play,
                            PulseTheme.colors.successSoft,
                            PulseTheme.colors.success,
                            Modifier.width(tileWidth),
                        )
                    }
                    info.end?.let {
                        MetricTile(
                            stringResource(R.string.pulse_fitness_workout_finished),
                            timeFormatter.format(it),
                            WorkoutSymbol.Stop,
                            PulseTheme.colors.errorSoft,
                            PulseTheme.colors.error,
                            Modifier.width(tileWidth),
                        )
                    }
                    info.durationSeconds?.let {
                        MetricTile(
                            stringResource(R.string.pulse_fitness_workout_duration),
                            formatWorkoutDuration(it).asString(),
                            WorkoutSymbol.Clock,
                            PulseTheme.colors.lavender.copy(alpha = .18f),
                            PulseTheme.colors.textPrimary,
                            Modifier.width(tileWidth),
                        )
                    }
                    info.calories?.let {
                        MetricTile(
                            stringResource(R.string.pulse_fitness_workout_energy),
                            stringResource(R.string.pulse_fitness_workout_kcal, it.toPlainString()),
                            WorkoutSymbol.Energy,
                            PulseTheme.colors.warningSoft,
                            PulseTheme.colors.warning,
                            Modifier.width(tileWidth),
                        )
                    }
                }
            }
        }
        if (!hasMetrics || info.imported) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                WorkoutGlyph(WorkoutSymbol.Info, PulseTheme.colors.textSecondary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!hasMetrics) {
                        PulseText(
                            stringResource(R.string.pulse_fitness_timing_and_energy_data_aren_t_available_for_this),
                            style = PulseTextStyle.Body2,
                            color = PulseTheme.colors.textSecondary,
                        )
                    }
                    if (info.imported) {
                        PulseText(
                            stringResource(R.string.pulse_fitness_linked_fitness_watch_workout),
                            style = PulseTextStyle.Body2,
                            color = PulseTheme.colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseList(
    exercises: List<WorkoutExercise>,
    enabled: Boolean,
    onEdit: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    modifier: Modifier,
) {
    PulseCard(modifier) {
        WorkoutSectionTitle(stringResource(R.string.pulse_fitness_workout_exercises))
        exercises.forEach { exercise ->
            key(exercise.id) { WorkoutExerciseCard(exercise, enabled, { onEdit(exercise.id) }, { onRemove(exercise.id) }) }
        }
    }
}

@Composable
private fun WorkoutExerciseCard(
    exercise: WorkoutExercise,
    enabled: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(20.dp), color = PulseTheme.colors.surface, border = BorderStroke(1.dp, PulseTheme.colors.divider)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BoxWithConstraints {
                val wide = maxWidth >= 560.dp
                val title: @Composable () -> Unit = { PulseText(exercise.name, Modifier.semantics { heading() }, PulseTextStyle.Heading2) }
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.weight(1f)) { title() }
                        exercise.catalog?.takeIf { it.instructions.isNotEmpty() }?.let { ExerciseInstructions(it) }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        title()
                        exercise.catalog?.takeIf { it.instructions.isNotEmpty() }?.let { ExerciseInstructions(it) }
                    }
                }
                // The catalogue has no description or image URL; never invent exercise-specific guidance.
            }
            BoxWithConstraints {
                val wide = maxWidth >= 560.dp
                val metadata: @Composable () -> Unit = {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        exercise.catalog?.equipment?.let {
                            MetadataTile(
                                stringResource(R.string.pulse_fitness_workout_equipment),
                                it,
                                WorkoutSymbol.Weight,
                            )
                        }
                        exercise.catalog
                            ?.muscles
                            ?.takeIf {
                                it.isNotEmpty()
                            }?.let {
                                MetadataTile(
                                    stringResource(R.string.pulse_fitness_workout_muscles),
                                    it.joinToString(),
                                    WorkoutSymbol.Muscle,
                                )
                            }
                    }
                }
                if (wide && exercise.catalog?.equipment?.contains("barbell", ignoreCase = true) == true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { metadata() }
                        WorkoutIllustration(R.drawable.workout_barbell, Modifier.width(180.dp).height(120.dp))
                    }
                } else {
                    metadata()
                }
            }
            PulseHorizontalDivider(color = PulseTheme.colors.divider)
            WorkoutSectionTitle(stringResource(R.string.pulse_fitness_workout_sets))
            BoxWithConstraints {
                val columns =
                    when {
                        maxWidth >= 600.dp -> 3
                        maxWidth >= 360.dp -> 2
                        else -> 1
                    }
                val tileWidth = (maxWidth - 12.dp * (columns - 1) - 1.dp) / columns
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    exercise.sets.forEach { set ->
                        Row(
                            Modifier.width(tileWidth).background(PulseTheme.colors.background, RoundedCornerShape(16.dp)).padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(40.dp).background(PulseTheme.colors.primarySoft, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                PulseText(set.number.toString(), style = PulseTextStyle.Label, color = PulseTheme.colors.onPrimarySoft)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                PulseText(
                                    pluralStringResource(R.plurals.pulse_fitness_workout_reps, set.reps, set.reps),
                                    style = PulseTextStyle.Label,
                                )
                                PulseText(
                                    set.weight?.let { stringResource(R.string.pulse_fitness_weight_kg, it.toPlainString()) }
                                        ?: stringResource(R.string.pulse_fitness_bodyweight),
                                    color = PulseTheme.colors.textSecondary,
                                    style = PulseTextStyle.Body2,
                                )
                            }
                        }
                    }
                }
            }
            PulseHorizontalDivider(color = PulseTheme.colors.divider)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkoutActionButton(stringResource(R.string.pulse_fitness_edit_exercise), WorkoutSymbol.Edit, onEdit, enabled)
                WorkoutActionButton(
                    stringResource(R.string.pulse_fitness_workout_remove),
                    WorkoutSymbol.Delete,
                    onRemove,
                    enabled,
                    destructive = true,
                )
            }
        }
    }
}

@Composable
private fun WorkoutSummary(
    detail: WorkoutDetail,
    modifier: Modifier,
) {
    PulseCard(modifier) {
        WorkoutSectionTitle(stringResource(R.string.pulse_fitness_workout_summary))
        val sets = detail.exercises.sumOf { it.sets.size }
        BoxWithConstraints {
            val compact = maxWidth >= 300.dp && maxWidth < 600.dp
            val exercises: @Composable () -> Unit = {
                SummaryMetric(
                    detail.exercises.size,
                    pluralStringResource(R.plurals.pulse_fitness_workout_exercises_label, detail.exercises.size),
                    WorkoutSymbol.Weight,
                    compact,
                )
            }
            val setCount: @Composable () -> Unit = {
                SummaryMetric(sets, pluralStringResource(R.plurals.pulse_fitness_workout_sets_label, sets), WorkoutSymbol.List, compact)
            }
            if (compact) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.weight(1f)) { exercises() }
                    Box(Modifier.weight(1f)) { setCount() }
                }
            } else {
                Column {
                    exercises()
                    setCount()
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    count: Int,
    label: String,
    symbol: WorkoutSymbol,
    compact: Boolean = false,
) {
    Row(
        Modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 20.dp),
    ) {
        Box(
            Modifier.size(if (compact) 48.dp else 64.dp).background(PulseTheme.colors.primarySoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            WorkoutGlyph(symbol, PulseTheme.colors.action)
        }
        Column {
            PulseText(
                count.toString(),
                Modifier.testTag(
                    if (symbol ==
                        WorkoutSymbol.List
                    ) {
                        "workout-summary-sets"
                    } else {
                        "workout-summary-exercises"
                    },
                ),
                style = PulseTextStyle.Heading2,
            )
            PulseText(label, color = PulseTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun MetadataTile(
    label: String,
    value: String,
    symbol: WorkoutSymbol,
) {
    Row(
        Modifier
            .widthIn(max = 280.dp)
            .background(
                if (symbol ==
                    WorkoutSymbol.Muscle
                ) {
                    PulseTheme.colors.errorSoft.copy(alpha = .4f)
                } else {
                    PulseTheme.colors.background
                },
                RoundedCornerShape(16.dp),
            ).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).background(
                if (symbol ==
                    WorkoutSymbol.Muscle
                ) {
                    PulseTheme.colors.errorSoft
                } else {
                    PulseTheme.colors.primarySoft
                },
                CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) {
            WorkoutGlyph(
                symbol,
                if (symbol ==
                    WorkoutSymbol.Muscle
                ) {
                    PulseTheme.colors.error
                } else {
                    PulseTheme.colors.action
                },
            )
        }
        Column {
            PulseText(label, style = PulseTextStyle.Body2, color = PulseTheme.colors.textSecondary)
            PulseText(value, style = PulseTextStyle.Label)
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    symbol: WorkoutSymbol,
    background: Color,
    tint: Color,
    modifier: Modifier,
) {
    Row(
        modifier.background(PulseTheme.colors.surface.copy(alpha = .8f), RoundedCornerShape(16.dp)).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).background(background, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            WorkoutGlyph(symbol, tint)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PulseText(label, style = PulseTextStyle.Body2)
            PulseText(value, style = PulseTextStyle.Label)
        }
    }
}

@Composable
private fun WorkoutSectionTitle(text: String) {
    PulseText(text, Modifier.semantics { heading() }, PulseTextStyle.Overline, PulseTheme.colors.action)
}

@Composable
private fun WorkoutIllustration(
    resource: Int,
    modifier: Modifier,
) {
    Image(painterResource(resource), contentDescription = null, modifier = modifier)
}
