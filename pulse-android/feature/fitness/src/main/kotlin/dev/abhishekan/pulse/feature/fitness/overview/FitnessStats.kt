package dev.abhishekan.pulse.feature.fitness.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.FitnessMetric
import dev.abhishekan.pulse.core.domain.model.FitnessOverview
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseTone
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCardPadding
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTileCard
import dev.abhishekan.pulse.feature.fitness.R
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import dev.abhishekan.pulse.core.ui.R as CoreUiR

private val summaryMetricIds = setOf("last_workout", "streak")

@Composable
internal fun FitnessSummary(
    overview: FitnessOverview,
    modifier: Modifier,
    wide: Boolean,
) {
    val metrics = overview.metrics.filter { it.id in summaryMetricIds }
    if (metrics.isEmpty()) return

    val workoutDates =
        remember(overview.workouts, overview.today) {
            overview.workouts.mapNotNull { it.date.takeIf { date -> date <= overview.today } }.toSet()
        }
    val colors = fitnessCalendarColors()
    val locale = LocalConfiguration.current.locales[0]
    val tile: @Composable (FitnessMetric, Modifier) -> Unit = { metric, tileModifier ->
        if (metric.id == "last_workout") {
            PulseTileCard(
                PulseTone(fitnessCalendarGradientStops.first(), fitnessCalendarGradientStops.last()),
                tileModifier,
                backdrop = {
                    Icon(
                        painterResource(CoreUiR.drawable.pulse_fitness),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 20.dp, y = 28.dp)
                                .size(160.dp)
                                .rotate(-12f),
                        tint = Color.White.copy(alpha = .2f),
                    )
                },
            ) {
                Column(Modifier.heightIn(min = 112.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatGlyph(metric.id, Modifier.size(21.dp), Color.White)
                        PulseText(metric.label, style = PulseTextStyle.Label, color = Color.White)
                    }
                    Column {
                        PulseText(metric.value, style = PulseTextStyle.Heading2, color = Color.White)
                        if (workoutDates.isNotEmpty()) PulseText(metric.subtitle, color = Color.White)
                    }
                }
            }
        } else {
            PulseCard(tileModifier, contentPadding = PulseCardPadding.medium) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatGlyph(metric.id, Modifier.size(21.dp), colors.muted)
                    PulseText(metric.label, style = PulseTextStyle.Label, color = colors.muted)
                }
                PulseText(metric.value, style = PulseTextStyle.Heading2)
                PulseText(metric.subtitle, color = colors.muted)
                val dateFormat = remember(locale) { DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale) }
                val summary =
                    (6L downTo 0L)
                        .map { offset ->
                            val date = overview.today.minusDays(offset)
                            val status =
                                stringResource(
                                    if (date in
                                        workoutDates
                                    ) {
                                        R.string.pulse_fitness_calendar_workout_logged
                                    } else {
                                        R.string.pulse_fitness_calendar_no_workouts
                                    },
                                )
                            "${date.format(dateFormat)}, $status"
                        }.joinToString(". ")
                val weekLabel = stringResource(R.string.pulse_fitness_summary_week)
                FlowRow(
                    Modifier.clearAndSetSemantics { contentDescription = "$weekLabel. $summary" },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (offset in 6L downTo 0L) {
                        val date = overview.today.minusDays(offset)
                        val logged = date in workoutDates
                        Box(
                            Modifier.size(28.dp).then(
                                if (logged) {
                                    Modifier.background(fitnessCalendarGradient, CircleShape)
                                } else {
                                    Modifier.background(colors.chip, CircleShape)
                                },
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            PulseText(
                                date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                                style = PulseTextStyle.Caption,
                                color = if (logged) Color.White else colors.muted,
                            )
                        }
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (!wide && maxWidth >= 600.dp && LocalDensity.current.fontScale < 1.5f) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                metrics.forEach { tile(it, Modifier.weight(1f)) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(if (wide) 24.dp else 16.dp)) {
                metrics.forEach { tile(it, Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable
internal fun FitnessStats(
    overview: FitnessOverview,
    canRefresh: Boolean,
    onRefresh: () -> Unit,
) {
    val metrics = overview.metrics.filterNot { it.id in summaryMetricIds }
    if (metrics.isEmpty() && overview.metrics.isNotEmpty()) return

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
    ) {
        PulseText(
            stringResource(R.string.pulse_fitness_stats_title),
            Modifier.semantics { heading() },
            style = PulseTextStyle.Title,
        )
        if (metrics.isEmpty()) {
            PulseText(
                stringResource(R.string.pulse_fitness_stats_unavailable),
                color = PulseTheme.colors.textSecondary,
            )
            PulseButton(
                stringResource(R.string.pulse_fitness_retry_stats),
                onRefresh,
                enabled = canRefresh,
                style = PulseButtonStyle.Text,
            )
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val fontScale = LocalDensity.current.fontScale
                // Base columns on the actual available pane width, including split-screen windows.
                val columns = ((maxWidth.value + 12) / (156 * fontScale + 12)).toInt().coerceIn(1, 4)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    metrics.chunked(columns).forEach { rowMetrics ->
                        Row(
                            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            rowMetrics.forEach { metric ->
                                FitnessStatCard(metric, Modifier.weight(1f).fillMaxHeight())
                            }
                            repeat(columns - rowMetrics.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FitnessStatCard(
    metric: FitnessMetric,
    modifier: Modifier = Modifier,
) {
    val colors = PulseTheme.colors
    PulseCard(
        modifier = modifier.semantics(mergeDescendants = true) {},
        contentPadding = PulseCardPadding.small,
    ) {
        Column(
            Modifier.heightIn(min = 188.dp),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
        ) {
            Surface(shape = PulseTokens.controlShape, color = colors.primarySoft) {
                StatGlyph(metric.id, Modifier.padding(PulseTokens.small).size(24.dp))
            }
            PulseText(
                if (metric.id == "preference") stringResource(R.string.pulse_fitness_most_common_workout_time) else metric.label,
                style = PulseTextStyle.Label,
            )
            PulseText(
                metric.value,
                style = if (metric.id == "preference" || metric.value.length > 14) PulseTextStyle.Heading3 else PulseTextStyle.Metric,
            )
            Spacer(Modifier.weight(1f))
            PulseText(metric.subtitle, style = PulseTextStyle.Caption, color = colors.textSecondary)
            metric.note?.takeIf { it.isNotBlank() }?.let { note ->
                PulseText(
                    stringResource(R.string.pulse_fitness_stats_coverage, note),
                    style = PulseTextStyle.Caption,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

/** Decorative glyphs identify metrics without implying a goal or progress that the API doesn't supply. */
@Composable
private fun StatGlyph(
    id: String,
    modifier: Modifier,
    color: Color = PulseTheme.colors.action,
) {
    Canvas(modifier) {
        val stroke = 1.8.dp.toPx()

        fun point(
            x: Float,
            y: Float,
        ) = Offset(size.width * x, size.height * y)

        fun line(
            x1: Float,
            y1: Float,
            x2: Float,
            y2: Float,
        ) = drawLine(color, point(x1, y1), point(x2, y2), stroke, StrokeCap.Round)
        when (id) {
            "last_workout", "trained", "preference" -> {
                drawCircle(color, size.minDimension * .36f, style = Stroke(stroke))
                line(.5f, .5f, .5f, .28f)
                line(.5f, .5f, .68f, if (id == "preference") .4f else .62f)
                if (id == "trained") line(.4f, .04f, .6f, .04f)
            }
            "attendance" -> {
                for (x in 0..2) {
                    for (y in 0..2) {
                        val alpha = if (x + y < 3) 1f else .3f
                        drawCircle(
                            color.copy(alpha = alpha),
                            size.minDimension * .07f,
                            point(.24f + x * .26f, .24f + y * .26f),
                        )
                    }
                }
            }
            "workouts", "volume" -> {
                line(.1f, .5f, .9f, .5f)
                line(.24f, .22f, .24f, .78f)
                line(.76f, .22f, .76f, .78f)
                line(.1f, .34f, .1f, .66f)
                line(.9f, .34f, .9f, .66f)
            }
            "streak" -> {
                line(.15f, .78f, .4f, .5f)
                line(.4f, .5f, .58f, .62f)
                line(.58f, .62f, .85f, .22f)
                line(.61f, .22f, .85f, .22f)
                line(.85f, .22f, .85f, .46f)
            }
            else -> {
                for (row in 0..2) {
                    drawRoundRect(color, point(.18f, .2f + row * .25f), Size(size.width * .64f, stroke), CornerRadius(stroke))
                }
            }
        }
    }
}
