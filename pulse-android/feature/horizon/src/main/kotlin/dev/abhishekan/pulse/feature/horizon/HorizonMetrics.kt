package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseIconButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.currencyText
import java.math.RoundingMode

@Composable
internal fun HorizonMetrics(
    data: HorizonOverview,
    enabled: Boolean,
    configure: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fitsRow = maxWidth >= 840.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        val cards: @Composable (Modifier) -> Unit = { modifier ->
            HorizonMetricCard(
                kind = MetricKind.Starting,
                title = stringResource(R.string.pulse_horizon_starting_pool_title),
                value = currencyText(data.baseAmount).asString(),
                description = stringResource(R.string.pulse_horizon_baseline_revenue),
                modifier = modifier,
                enabled = enabled,
                configure = configure,
            )
            HorizonMetricCard(
                kind = MetricKind.Committed,
                title = stringResource(R.string.pulse_horizon_total_committed),
                value = currencyText(data.deductionsTotal + data.subscriptionBurn).asString(),
                description =
                    stringResource(
                        R.string.pulse_horizon_percent_of_pool,
                        data.committedPercent.setScale(1, RoundingMode.HALF_UP).toPlainString(),
                    ),
                modifier = modifier,
                ratio = data.committedPercent.toFloat(),
            )
            HorizonMetricCard(
                kind = MetricKind.Available,
                title = stringResource(R.string.pulse_horizon_net_available_pool),
                value = currencyText(data.remaining).asString(),
                description = stringResource(R.string.pulse_horizon_available_pool_description),
                modifier = modifier,
                deficit = data.isOvercommitted,
            )
        }
        if (fitsRow) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                cards(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                cards(Modifier.fillMaxWidth())
            }
        }
    }
}

private enum class MetricKind { Starting, Committed, Available }

@Composable
private fun HorizonMetricCard(
    kind: MetricKind,
    title: String,
    value: String,
    description: String,
    modifier: Modifier,
    ratio: Float = 0f,
    deficit: Boolean = false,
    enabled: Boolean = true,
    configure: () -> Unit = {},
) {
    val dark = PulseTheme.isDark
    val background =
        when (kind) {
            MetricKind.Starting -> if (dark) Color(0xFF3B321C) else Color(0xFFFFEFC2)
            MetricKind.Committed -> if (dark) Color(0xFF3E2B28) else Color(0xFFFCE7DF)
            MetricKind.Available ->
                if (deficit) {
                    if (dark) Color(0xFF42282B) else Color(0xFFFFE3E5)
                } else {
                    if (dark) Color(0xFF203A2B) else Color(0xFFE0F3E1)
                }
        }
    val accent =
        when (kind) {
            MetricKind.Starting -> if (dark) Color(0xFFFFD36C) else Color(0xFFF4B82F)
            MetricKind.Committed -> if (dark) Color(0xFFF4AB97) else Color(0xFFE6957E)
            MetricKind.Available ->
                if (deficit) {
                    if (dark) Color(0xFFFFA3AA) else Color(0xFFB93D4A)
                } else {
                    if (dark) Color(0xFF8AD49C) else Color(0xFF61B977)
                }
        }
    val foreground = if (dark) Color(0xFFF8F4EB) else Color(0xFF152018)
    Surface(
        modifier.testTag("horizon-metric-${kind.name.lowercase()}"),
        shape = RoundedCornerShape(20.dp),
        color = background,
        contentColor = foreground,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier.background(accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).size(36.dp),
                    contentAlignment = Alignment.Center,
                ) { MetricIcon(kind, foreground) }
                PulseText(title, Modifier.weight(1f), PulseTextStyle.Label, foreground)
                if (kind == MetricKind.Starting) {
                    PulseIconButton(
                        stringResource(R.string.pulse_horizon_configure_starting_pool),
                        configure,
                        enabled = enabled,
                    ) { EditPoolIcon(foreground.copy(alpha = if (enabled) 1f else 0.4f)) }
                }
            }
            // Give money the full card width, including with large fonts and long currencies.
            PulseText(value, style = PulseTextStyle.Heading2, color = foreground)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (kind == MetricKind.Available) {
                        PulseText(
                            stringResource(if (deficit) R.string.pulse_horizon_deficit else R.string.pulse_horizon_surplus),
                            Modifier
                                .background(
                                    accent.copy(alpha = 0.25f),
                                    RoundedCornerShape(50),
                                ).padding(horizontal = 8.dp, vertical = 4.dp),
                            PulseTextStyle.Caption,
                            foreground,
                        )
                    }
                    PulseText(description, style = PulseTextStyle.Caption, color = foreground.copy(alpha = 0.8f))
                }
                if (kind == MetricKind.Committed) {
                    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(64.dp)) {
                            val stroke = Stroke(7.dp.toPx(), cap = StrokeCap.Round)
                            val inset = 4.dp.toPx()
                            val ringSize = Size(size.width - inset * 2, size.height - inset * 2)
                            drawArc(accent.copy(alpha = 0.25f), 0f, 360f, false, Offset(inset, inset), ringSize, style = stroke)
                            drawArc(accent, -90f, ratio.coerceIn(0f, 100f) * 3.6f, false, Offset(inset, inset), ringSize, style = stroke)
                        }
                        PulseText(
                            "${ratio.toBigDecimal().setScale(0, RoundingMode.HALF_UP)}%",
                            style = PulseTextStyle.Caption,
                            color = foreground,
                        )
                    }
                } else {
                    Canvas(Modifier.size(44.dp)) {
                        val bars = if (kind == MetricKind.Starting) listOf(0.45f, 0.7f, 1f) else listOf(1f, 1f)
                        bars.forEachIndexed { index, height ->
                            drawRoundRect(
                                accent.copy(alpha = 0.7f),
                                Offset(index * size.width / bars.size, size.height * (1f - height)),
                                Size(size.width / bars.size - 4.dp.toPx(), size.height * height),
                                CornerRadius(3.dp.toPx()),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricIcon(
    kind: MetricKind,
    color: Color,
) {
    Canvas(Modifier.size(20.dp)) {
        val stroke = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        when (kind) {
            MetricKind.Starting -> {
                drawRoundRect(color, Offset(w * 0.1f, h * 0.25f), Size(w * 0.8f, h * 0.6f), CornerRadius(w * 0.1f), style = stroke)
                drawLine(color, Offset(w * 0.15f, h * 0.25f), Offset(w * 0.7f, h * 0.1f), stroke.width)
                drawLine(color, Offset(w * 0.65f, h * 0.55f), Offset(w * 0.85f, h * 0.55f), stroke.width)
            }
            MetricKind.Committed -> {
                drawRoundRect(color, Offset(w * 0.2f, h * 0.45f), Size(w * 0.6f, h * 0.45f), CornerRadius(w * 0.08f), style = stroke)
                drawArc(color, 180f, 180f, false, Offset(w * 0.32f, h * 0.1f), Size(w * 0.36f, h * 0.65f), style = stroke)
            }
            MetricKind.Available -> {
                val path =
                    Path().apply {
                        moveTo(w * 0.5f, h * 0.1f)
                        lineTo(w * 0.9f, h * 0.32f)
                        lineTo(w * 0.5f, h * 0.55f)
                        lineTo(w * 0.1f, h * 0.32f)
                        close()
                        moveTo(w * 0.1f, h * 0.52f)
                        lineTo(w * 0.5f, h * 0.75f)
                        lineTo(w * 0.9f, h * 0.52f)
                        moveTo(w * 0.1f, h * 0.72f)
                        lineTo(w * 0.5f, h * 0.95f)
                        lineTo(w * 0.9f, h * 0.72f)
                    }
                drawPath(path, color, style = stroke)
            }
        }
    }
}

@Composable
private fun EditPoolIcon(color: Color) {
    Canvas(Modifier.size(20.dp)) {
        val path =
            Path().apply {
                moveTo(size.width * 0.2f, size.height * 0.8f)
                lineTo(size.width * 0.25f, size.height * 0.58f)
                lineTo(size.width * 0.7f, size.height * 0.13f)
                lineTo(size.width * 0.87f, size.height * 0.3f)
                lineTo(size.width * 0.42f, size.height * 0.75f)
                close()
            }
        drawPath(path, color, style = Stroke(1.6.dp.toPx()))
    }
}
