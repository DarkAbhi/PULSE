package dev.abhishekan.pulse.feature.fitness.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.fitness.R

internal val fitnessCalendarGradientStops = listOf(Color(0xFF3A2CD9), Color(0xFFA420BD))
internal val fitnessCalendarGradient = Brush.linearGradient(fitnessCalendarGradientStops)

internal data class FitnessCalendarColors(
    val muted: Color,
    val chip: Color,
    val logged: Color,
    val dot: Color,
    val today: Color,
)

@Composable
internal fun fitnessCalendarColors() =
    if (PulseTheme.isDark) {
        FitnessCalendarColors(Color(0xFFB7B0D1), Color(0xFF2B2749), Color(0xFF193953), Color(0xFF6ADAF7), Color(0xFFFF55D1))
    } else {
        FitnessCalendarColors(PulseTheme.colors.textSecondary, Color(0xFFEEEDF6), Color(0xFFE6EBFF), Color(0xFF3E58DC), Color(0xFFDC229F))
    }

internal enum class FitnessSymbol { Previous, Next }

@Composable
internal fun FitnessGlyph(
    symbol: FitnessSymbol,
    color: Color,
    dimension: Dp = 20.dp,
) {
    Canvas(Modifier.size(dimension)) {
        val direction = if ((symbol == FitnessSymbol.Next) == (layoutDirection == LayoutDirection.Ltr)) 1 else -1
        val start = size.width * (.5f - .125f * direction)
        val end = size.width * (.5f + .125f * direction)
        val middle = Offset(end, size.height * .5f)
        val stroke = (dimension / 12).toPx()
        drawLine(color, Offset(start, size.height * .25f), middle, stroke, StrokeCap.Round)
        drawLine(color, middle, Offset(start, size.height * .75f), stroke, StrokeCap.Round)
    }
}

@Composable
internal fun CalendarControl(
    label: String,
    symbol: FitnessSymbol,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    PulseButton(
        label,
        onClick,
        modifier = Modifier.background(PulseTheme.colors.surfaceMuted, CircleShape),
        enabled = enabled,
        style = PulseButtonStyle.Icon,
    ) {
        FitnessGlyph(symbol, if (enabled) PulseTheme.colors.textPrimary else fitnessCalendarColors().muted)
    }
}

@Composable
internal fun FitnessLogWorkoutButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.alpha(if (enabled) 1f else .5f),
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
    ) {
        Box(
            Modifier
                .heightIn(min = 48.dp)
                .background(fitnessCalendarGradient)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            PulseText(stringResource(R.string.pulse_fitness_log_workout), style = PulseTextStyle.Label, color = Color.White)
        }
    }
}
