package dev.abhishekan.pulse.feature.fitness

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCalendarDay
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

internal enum class FitnessSymbol { Previous, Next, Calendar }

@Composable
internal fun FitnessGlyph(
    symbol: FitnessSymbol,
    color: Color,
    dimension: Dp = 24.dp,
) {
    Canvas(Modifier.size(dimension)) {
        val stroke = 2.dp.toPx()
        if (symbol == FitnessSymbol.Calendar) {
            drawRoundRect(
                color,
                Offset(size.width * .2f, size.height * .25f),
                Size(size.width * .6f, size.height * .6f),
                CornerRadius(2.dp.toPx()),
                style = Stroke(stroke),
            )
            drawLine(color, Offset(size.width * .2f, size.height * .42f), Offset(size.width * .8f, size.height * .42f), stroke)
            for (x in listOf(.35f, .65f)) {
                drawLine(color, Offset(size.width * x, size.height * .15f), Offset(size.width * x, size.height * .32f), stroke)
            }
        } else {
            val direction = if (symbol == FitnessSymbol.Next) 1 else -1
            val start = size.width * (.5f - .12f * direction)
            val end = size.width * (.5f + .12f * direction)
            drawLine(color, Offset(start, size.height * .3f), Offset(end, size.height * .5f), stroke)
            drawLine(color, Offset(end, size.height * .5f), Offset(start, size.height * .7f), stroke)
        }
    }
}

@Composable
internal fun CalendarControl(
    label: String,
    symbol: FitnessSymbol,
    enabled: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val glyph: @Composable () -> Unit = {
        FitnessGlyph(
            symbol,
            if (enabled) PulseTheme.colors.textPrimary else PulseTheme.colors.textSecondary,
            dimension = if (compact) 20.dp else 24.dp,
        )
    }
    if (compact) {
        PulseButton(label, onClick, enabled = enabled, style = PulseButtonStyle.Icon) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = PulseTokens.controlShape,
                color = PulseTheme.colors.surface,
                border = BorderStroke(1.dp, PulseTheme.colors.divider),
            ) {
                Box(contentAlignment = Alignment.Center) { glyph() }
            }
        }
    } else {
        // Keep the full-size outline outside IconButton's circular clipping layer.
        Surface(
            shape = PulseTokens.controlShape,
            color = PulseTheme.colors.surface,
            border = BorderStroke(1.dp, PulseTheme.colors.divider),
        ) {
            PulseButton(label, onClick, enabled = enabled, icon = glyph, style = PulseButtonStyle.Icon)
        }
    }
}

@Composable
internal fun CalendarLegend(
    label: String,
    color: Color,
    outlined: Boolean = false,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            Modifier.size(if (outlined) 16.dp else 8.dp),
            shape = CircleShape,
            color = if (outlined) Color.Transparent else color,
            border = if (outlined) BorderStroke(1.dp, color) else null,
        ) {}
        PulseText(label, style = PulseTextStyle.Caption, color = PulseTheme.colors.textSecondary)
    }
}

@Composable
internal fun FitnessCalendarDay(
    label: String,
    description: String,
    selected: Boolean,
    today: Boolean,
    hasActivity: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    inMonth: Boolean = true,
) {
    PulseCalendarDay(
        label = label,
        description = description,
        selected = selected,
        today = today,
        hasActivity = hasActivity,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        inMonth = inMonth,
    )
}

@Composable
internal fun CompactTodayControl(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color.Transparent,
        contentColor = PulseTheme.colors.action,
    ) {
        Box(Modifier.size(PulseTokens.target), contentAlignment = Alignment.Center) {
            Surface(shape = PulseTokens.controlShape, color = PulseTheme.colors.primarySoft) {
                Box(Modifier.size(width = 40.dp, height = 32.dp), contentAlignment = Alignment.Center) {
                    PulseText(label, style = PulseTextStyle.Caption)
                }
            }
        }
    }
}
