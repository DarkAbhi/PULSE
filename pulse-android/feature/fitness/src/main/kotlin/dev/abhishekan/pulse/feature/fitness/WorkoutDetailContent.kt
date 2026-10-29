package dev.abhishekan.pulse.feature.fitness

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.domain.model.FitnessDates
import dev.abhishekan.pulse.core.domain.model.WorkoutDetail
import dev.abhishekan.pulse.core.domain.model.WorkoutExercise
import dev.abhishekan.pulse.core.domain.model.WorkoutInfo
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import java.time.format.DateTimeFormatter

@Composable
internal fun WorkoutDetailContent(
    detail: WorkoutDetail,
    enabled: Boolean,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onDelete: () -> Unit,
) {
    WorkoutInformation(detail.info)
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
                        WorkoutAction(
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
        WorkoutAction(
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
                    WorkoutAction(
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
private fun WorkoutInformation(info: WorkoutInfo) {
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
            info.createdAt.atZone(FitnessDates.zone).format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
            Modifier.semantics {
                heading()
            },
            PulseTextStyle.Heading2,
        )
        PulseText(
            stringResource(
                if (info.imported) {
                    R.string.pulse_fitness_linked_apple_watch_workout
                } else {
                    R.string.pulse_fitness_recorded_gym_visit
                },
            ),
        )
        if (hasMetrics) {
            val formatter = DateTimeFormatter.ofPattern("d MMM · HH:mm").withZone(FitnessDates.zone)
            BoxWithConstraints(Modifier.padding(vertical = 8.dp)) {
                val columns = if (maxWidth >= 720.dp) 4 else 2
                val tileWidth = (maxWidth - 12.dp * (columns - 1) - 1.dp) / columns
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    info.start?.let {
                        MetricTile(
                            stringResource(R.string.pulse_fitness_workout_started),
                            formatter.format(it),
                            WorkoutSymbol.Play,
                            PulseTheme.colors.successSoft,
                            PulseTheme.colors.success,
                            Modifier.width(tileWidth),
                        )
                    }
                    info.end?.let {
                        MetricTile(
                            stringResource(R.string.pulse_fitness_workout_finished),
                            formatter.format(it),
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
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
                            stringResource(R.string.pulse_fitness_workout_imported_read_only),
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
                        exercise.catalog?.let { ExerciseInstructions(it) }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        title()
                        exercise.catalog?.let { ExerciseInstructions(it) }
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
                WorkoutAction(stringResource(R.string.pulse_fitness_edit_exercise), WorkoutSymbol.Edit, onEdit, enabled)
                WorkoutAction(
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
internal fun ExerciseInstructions(exercise: CatalogExercise) {
    if (exercise.instructions.isEmpty()) return
    EditorInstructions(exercise)
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

@Composable
internal fun WorkoutAction(
    text: String,
    symbol: WorkoutSymbol,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    destructive: Boolean = false,
    soft: Boolean = false,
) {
    PulseButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        variant =
            when {
                destructive -> PulseButtonVariant.DestructiveSoft
                primary -> PulseButtonVariant.Primary
                soft -> PulseButtonVariant.Soft
                else -> PulseButtonVariant.Secondary
            },
        leadingIcon = {
            WorkoutGlyph(symbol, androidx.compose.material3.LocalContentColor.current)
        },
    )
}

internal enum class WorkoutSymbol { Add, Play, Stop, Clock, Energy, Info, Weight, Muscle, List, Edit, Delete, Refresh, Document }

@Composable
internal fun WorkoutGlyph(
    symbol: WorkoutSymbol,
    color: Color,
) {
    Canvas(Modifier.size(24.dp)) {
        val u = size.width / 24f
        val stroke = 1.8f * u

        fun line(
            x: Float,
            y: Float,
            xx: Float,
            yy: Float,
        ) = drawLine(color, Offset(x * u, y * u), Offset(xx * u, yy * u), stroke)

        fun rect(
            x: Float,
            y: Float,
            w: Float,
            h: Float,
            fill: Boolean = false,
        ) = drawRoundRect(
            color,
            Offset(x * u, y * u),
            Size(
                w * u,
                h * u,
            ),
            CornerRadius(1.5f * u),
            style = if (fill) androidx.compose.ui.graphics.drawscope.Fill else Stroke(stroke),
        )
        when (symbol) {
            WorkoutSymbol.Add -> {
                line(12f, 3f, 12f, 21f)
                line(3f, 12f, 21f, 12f)
            }
            WorkoutSymbol.Stop -> rect(5f, 5f, 14f, 14f, true)
            WorkoutSymbol.Play ->
                drawPath(
                    Path().apply {
                        moveTo(7 * u, 4 * u)
                        lineTo(20 * u, 12 * u)
                        lineTo(7 * u, 20 * u)
                        close()
                    },
                    color,
                )
            WorkoutSymbol.Clock, WorkoutSymbol.Info -> {
                drawCircle(color, 9 * u, style = Stroke(stroke))
                if (symbol ==
                    WorkoutSymbol.Clock
                ) {
                    line(12f, 6f, 12f, 12f)
                    line(12f, 12f, 17f, 15f)
                } else {
                    line(12f, 10f, 12f, 17f)
                    drawCircle(
                        color,
                        u,
                        Offset(
                            12 * u,
                            7 * u,
                        ),
                    )
                }
            }
            WorkoutSymbol.Weight -> {
                line(2f, 12f, 22f, 12f)
                rect(4f, 6f, 4f, 12f, true)
                rect(16f, 6f, 4f, 12f, true)
            }
            WorkoutSymbol.List -> for (y in listOf(6f, 12f, 18f)) {
                drawCircle(color, 1.5f * u, Offset(4 * u, y * u))
                line(9f, y, 21f, y)
            }
            WorkoutSymbol.Muscle -> {
                line(8f, 3f, 6f, 12f)
                line(6f, 12f, 8f, 21f)
                line(16f, 3f, 18f, 12f)
                line(18f, 12f, 16f, 21f)
                line(12f, 4f, 12f, 19f)
            }
            WorkoutSymbol.Delete -> {
                rect(6f, 7f, 12f, 14f)
                line(3f, 5f, 21f, 5f)
                line(9f, 2f, 15f, 2f)
                line(10f, 10f, 10f, 18f)
                line(14f, 10f, 14f, 18f)
            }
            WorkoutSymbol.Document -> {
                rect(5f, 2f, 14f, 20f)
                for (y in listOf(8f, 12f, 16f)) line(8f, y, 16f, y)
            }
            WorkoutSymbol.Refresh -> {
                drawArc(color, 35f, 285f, false, Offset(3 * u, 3 * u), Size(18 * u, 18 * u), style = Stroke(stroke))
                line(20f, 3f, 20f, 9f)
                line(14f, 9f, 20f, 9f)
            }
            WorkoutSymbol.Edit -> {
                line(4f, 17f, 17f, 4f)
                line(7f, 20f, 20f, 7f)
                line(17f, 4f, 20f, 7f)
                line(4f, 17f, 3f, 21f)
                line(3f, 21f, 7f, 20f)
            }
            WorkoutSymbol.Energy ->
                drawPath(
                    Path().apply {
                        moveTo(14 * u, 2 * u)
                        lineTo(5 * u, 14 * u)
                        lineTo(11 * u, 14 * u)
                        lineTo(10 * u, 22 * u)
                        lineTo(
                            20 * u,
                            9 * u,
                        )
                        lineTo(14 * u, 9 * u)
                        close()
                    },
                    color,
                )
        }
    }
}
