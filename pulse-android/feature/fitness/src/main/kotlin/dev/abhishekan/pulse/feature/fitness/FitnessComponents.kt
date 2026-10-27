package dev.abhishekan.pulse.feature.fitness

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseIconButton
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
        PulseIconButton(label, onClick, enabled = enabled) {
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
            PulseIconButton(label, onClick, enabled = enabled, icon = glyph)
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
    val colors = PulseTheme.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .defaultMinSize(minHeight = 56.dp)
                .clearAndSetSemantics {
                    contentDescription = description
                    this.selected = selected
                    role = Role.Button
                    if (enabled) {
                        onClick {
                            onClick()
                            true
                        }
                    } else {
                        disabled()
                    }
                },
        shape = PulseTokens.controlShape,
        color = if (selected) colors.primary else Color.Transparent,
        contentColor =
            when {
                selected -> colors.onPrimary
                inMonth -> colors.textPrimary
                else -> colors.textSecondary
            },
        border = if (today) BorderStroke(2.dp, colors.action) else null,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = PulseTokens.small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny),
        ) {
            PulseText(label, style = PulseTextStyle.Label)
            Box(Modifier.size(4.dp)) {
                if (hasActivity) {
                    Surface(
                        Modifier.size(4.dp),
                        shape = CircleShape,
                        color = if (selected) colors.onPrimary else colors.success,
                    ) {}
                }
            }
        }
    }
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
