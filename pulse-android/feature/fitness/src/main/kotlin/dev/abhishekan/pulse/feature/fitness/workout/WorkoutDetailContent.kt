package dev.abhishekan.pulse.feature.fitness.workout

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.WorkoutDetail
import dev.abhishekan.pulse.core.domain.model.WorkoutInfo
import dev.abhishekan.pulse.core.ui.PulseShapes
import dev.abhishekan.pulse.core.ui.PulseSpacing
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.fitness.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun LazyListScope.workoutDetailItems(
    detail: WorkoutDetail,
    timeZone: ZoneId,
    enabled: Boolean,
    wide: Boolean,
    expandedExerciseId: Long?,
    onToggle: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onDelete: () -> Unit,
) {
    item(key = "workout-information", contentType = "information") {
        Box(Modifier.fillMaxWidth().padding(bottom = PulseSpacing.cardPadding)) {
            WorkoutInformation(detail.info, timeZone)
        }
    }
    if (detail.exercises.isEmpty()) {
        item(key = "workout-empty", contentType = "empty") {
            EmptyWorkoutExercises(Modifier.fillMaxWidth().padding(bottom = PulseSpacing.cardPadding))
        }
    } else {
        if (!wide) {
            item(key = "workout-summary", contentType = "summary") {
                WorkoutSummary(detail, Modifier.fillMaxWidth().padding(bottom = PulseSpacing.comfortable))
            }
        }
        itemsIndexed(
            detail.exercises,
            key = { _, exercise -> exercise.id },
            contentType = { index, _ -> if (wide && index == 0) "exercise-with-summary" else "exercise" },
        ) { index, exercise ->
            val gap = if (index == detail.exercises.lastIndex) PulseSpacing.cardPadding else PulseSpacing.compact
            val card: @Composable (Modifier) -> Unit = { modifier ->
                WorkoutExerciseCard(
                    exercise,
                    number = index + 1,
                    expanded = exercise.id == expandedExerciseId,
                    enabled = enabled,
                    onToggle = { onToggle(exercise.id) },
                    onEdit = { onEdit(exercise.id) },
                    onRemove = { onRemove(exercise.id) },
                    modifier = modifier,
                )
            }
            if (wide && index == 0) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = gap),
                    horizontalArrangement = Arrangement.spacedBy(PulseSpacing.comfortable),
                ) {
                    card(Modifier.weight(1f))
                    WorkoutSummary(detail, Modifier.width(WorkoutSummaryWidth))
                }
            } else {
                val summarySpace = if (wide) WorkoutSummaryWidth + PulseSpacing.comfortable else PulseSpacing.none
                card(Modifier.fillMaxWidth().padding(end = summarySpace, bottom = gap))
            }
        }
    }
    item(key = "workout-management", contentType = "management") {
        WorkoutManagement(enabled, onDelete)
    }
}

private val WorkoutSummaryWidth = 264.dp

@Composable
private fun EmptyWorkoutExercises(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val wide = maxWidth >= 640.dp
        val copy: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(PulseSpacing.medium)) {
                PulseText(stringResource(R.string.pulse_fitness_workout_empty), style = PulseTextStyle.Heading2)
                PulseText(
                    stringResource(R.string.pulse_fitness_choose_a_catalogue_exercise_or_record_your_own_name),
                    color = PulseTheme.colors.textSecondary,
                )
            }
        }
        if (wide) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).padding(vertical = PulseSpacing.cardPadding)) { copy() }
                WorkoutIllustration(R.drawable.workout_empty, Modifier.width(260.dp).height(220.dp))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(PulseSpacing.medium)) {
                WorkoutIllustration(R.drawable.workout_empty, Modifier.fillMaxWidth().height(160.dp))
                copy()
            }
        }
    }
}

@Composable
private fun WorkoutManagement(
    enabled: Boolean,
    onDelete: () -> Unit,
) {
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
private fun WorkoutInformation(
    info: WorkoutInfo,
    timeZone: ZoneId,
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale, timeZone) { DateTimeFormatter.ofPattern("EEE, d MMM yyyy", locale).withZone(timeZone) }
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
            ).padding(PulseSpacing.comfortable),
        verticalArrangement = Arrangement.spacedBy(PulseSpacing.compact),
    ) {
        Column {
            WorkoutSectionTitle(stringResource(R.string.pulse_fitness_your_workout))
            PulseText(dateFormatter.format(info.createdAt), Modifier.semantics { heading() }, PulseTextStyle.Heading3)
        }
        if (!info.imported) {
            PulseText(stringResource(R.string.pulse_fitness_recorded_gym_visit))
        }
        if (info.start != null || info.end != null || info.durationSeconds != null) {
            WorkoutTiming(info, timeZone)
        }
        info.calories?.let { WorkoutEnergy(it.toPlainString()) }
        if (!hasMetrics || info.imported) {
            Row(horizontalArrangement = Arrangement.spacedBy(PulseSpacing.small), verticalAlignment = Alignment.CenterVertically) {
                WorkoutGlyph(WorkoutSymbol.Info, PulseTheme.colors.textSecondary, Modifier.size(16.dp))
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
private fun WorkoutTiming(
    info: WorkoutInfo,
    timeZone: ZoneId,
) {
    val locale = LocalConfiguration.current.locales[0]
    val timeFormatter = remember(locale, timeZone) { DateTimeFormatter.ofPattern("HH:mm", locale).withZone(timeZone) }
    val dayFormatter = remember(locale, timeZone) { DateTimeFormatter.ofPattern("d MMM", locale).withZone(timeZone) }
    val workoutDay = info.createdAt.atZone(timeZone).toLocalDate()
    val start = info.start
    val end = info.end
    val duration = info.durationSeconds?.let { formatWorkoutDuration(it, compact = true).asString() }

    BoxWithConstraints(Modifier.workoutMetricPanel()) {
        val hasTimes = start != null || end != null
        val inlineDuration = maxWidth >= 220.dp * LocalDensity.current.fontScale
        Column(verticalArrangement = Arrangement.spacedBy(PulseSpacing.small), horizontalAlignment = Alignment.CenterHorizontally) {
            if (hasTimes) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PulseSpacing.tiny),
                ) {
                    Box(Modifier.weight(1f)) {
                        if (start != null) {
                            WorkoutTime(
                                instant = start,
                                timeFormatter = timeFormatter,
                                date = start.takeUnless { it.atZone(timeZone).toLocalDate() == workoutDay }?.let(dayFormatter::format),
                                finished = false,
                            )
                        }
                    }
                    if (duration != null && inlineDuration) {
                        Row(
                            Modifier.weight(1f).align(Alignment.CenterVertically),
                            horizontalArrangement = Arrangement.spacedBy(PulseSpacing.tiny, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val showConnectors = duration.length <= 6
                            if (showConnectors) Box(Modifier.size(PulseSpacing.tiny, 1.dp).background(PulseTheme.colors.divider))
                            WorkoutDurationBadge(duration, Modifier.weight(1f, fill = false))
                            if (showConnectors) Box(Modifier.size(PulseSpacing.tiny, 1.dp).background(PulseTheme.colors.divider))
                        }
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        if (end != null) {
                            WorkoutTime(
                                instant = end,
                                timeFormatter = timeFormatter,
                                date = end.takeUnless { it.atZone(timeZone).toLocalDate() == workoutDay }?.let(dayFormatter::format),
                                finished = true,
                            )
                        }
                    }
                }
            }
            if (duration != null && (!hasTimes || !inlineDuration)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    WorkoutDurationBadge(duration)
                }
            }
        }
    }
}

@Composable
private fun WorkoutTime(
    instant: Instant,
    timeFormatter: DateTimeFormatter,
    date: String?,
    finished: Boolean,
) {
    val colors = PulseTheme.colors
    Column(horizontalAlignment = if (finished) Alignment.End else Alignment.Start) {
        Row(horizontalArrangement = Arrangement.spacedBy(PulseSpacing.tiny), verticalAlignment = Alignment.CenterVertically) {
            if (!finished) {
                WorkoutMetricIcon(WorkoutSymbol.Play, colors.successSoft, colors.success, compact = true)
            }
            PulseText(
                stringResource(if (finished) R.string.pulse_fitness_workout_finished else R.string.pulse_fitness_workout_started),
                Modifier.weight(1f, fill = false),
                style = PulseTextStyle.Caption,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (finished) {
                WorkoutMetricIcon(WorkoutSymbol.Stop, colors.errorSoft, colors.error, compact = true)
            }
        }
        PulseText(timeFormatter.format(instant), style = PulseTextStyle.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        date?.let {
            PulseText(it, style = PulseTextStyle.Caption, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun WorkoutDurationBadge(
    duration: String,
    modifier: Modifier = Modifier,
) {
    PulseText(
        duration,
        modifier
            .background(PulseTheme.colors.primary, CircleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        style = PulseTextStyle.Label2,
        color = PulseTheme.colors.onPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun WorkoutEnergy(amount: String) {
    val colors = PulseTheme.colors
    val formatted = stringResource(R.string.pulse_fitness_workout_kcal, amount)
    val amountStyle =
        PulseTheme.typography.title1
            .toSpanStyle()
            .copy(color = colors.textPrimary)
    Row(
        Modifier.workoutMetricPanel(),
        horizontalArrangement = Arrangement.spacedBy(PulseSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WorkoutMetricIcon(WorkoutSymbol.Energy, colors.warningSoft, colors.warning)
        PulseText(stringResource(R.string.pulse_fitness_workout_energy), style = PulseTextStyle.Caption, color = colors.textSecondary)
        Text(
            buildAnnotatedString {
                append(formatted)
                val amountStart = formatted.indexOf(amount)
                if (amountStart >= 0) addStyle(amountStyle, amountStart, amountStart + amount.length)
            },
            Modifier.weight(1f),
            color = colors.textSecondary,
            style = PulseTheme.typography.caption,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WorkoutMetricIcon(
    symbol: WorkoutSymbol,
    background: Color,
    tint: Color,
    compact: Boolean = false,
) {
    Box(
        Modifier
            .size(if (compact) PulseSpacing.comfortable else PulseSpacing.large)
            .background(background, if (compact) PulseShapes.small else PulseShapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        WorkoutGlyph(symbol, tint, Modifier.size(if (compact) PulseSpacing.compact else PulseSpacing.medium))
    }
}

@Composable
private fun Modifier.workoutMetricPanel(): Modifier =
    fillMaxWidth()
        .background(PulseTheme.colors.surface.copy(alpha = .8f), PulseShapes.extraLarge)
        .padding(PulseSpacing.compact)

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
