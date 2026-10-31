package dev.abhishekan.pulse.feature.fitness

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.FitnessMetric
import dev.abhishekan.pulse.core.domain.model.FitnessOverview
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

@Composable
internal fun FitnessStats(overview: FitnessOverview) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny)) {
            PulseText(
                stringResource(R.string.pulse_fitness_stats_title),
                Modifier.semantics { heading() },
                style = PulseTextStyle.Title,
            )
            PulseText(
                stringResource(R.string.pulse_fitness_stats_period, overview.today.year),
                style = PulseTextStyle.Caption,
                color = PulseTheme.colors.textSecondary,
            )
        }
        if (overview.metrics.isEmpty()) {
            PulseText(
                stringResource(R.string.pulse_fitness_stats_unavailable),
                color = PulseTheme.colors.textSecondary,
            )
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val fontScale = LocalDensity.current.fontScale
                // Base columns on the actual available pane width, including split-screen windows.
                val columns = ((maxWidth.value + 12) / (156 * fontScale + 12)).toInt().coerceIn(1, 4)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    overview.metrics.chunked(columns).forEach { metrics ->
                        Row(
                            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            metrics.forEach { metric ->
                                FitnessStatCard(metric, Modifier.weight(1f).fillMaxHeight())
                            }
                            repeat(columns - metrics.size) { Spacer(Modifier.weight(1f)) }
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
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = PulseTokens.cardShape,
        color = colors.surface,
        contentColor = colors.textPrimary,
        border = BorderStroke(1.dp, colors.divider),
    ) {
        Column(
            Modifier.heightIn(min = 188.dp).padding(PulseTokens.medium),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
        ) {
            Surface(shape = PulseTokens.controlShape, color = colors.primarySoft) {
                StatGlyph(metric.id, Modifier.padding(PulseTokens.small).size(24.dp))
            }
            PulseText(metric.label, style = PulseTextStyle.Label)
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
) {
    val color = PulseTheme.colors.action
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
