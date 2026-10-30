package dev.abhishekan.pulse.core.ui.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme

@Composable
fun PulseHorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = PulseTheme.colors.divider,
) {
    HorizontalDivider(modifier = modifier, thickness = thickness, color = color)
}

@Composable
fun PulseLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = PulseTheme.colors.action,
    trackColor: Color = PulseTheme.colors.surfaceMuted,
    strokeCap: StrokeCap = StrokeCap.Round,
    gapSize: Dp = 4.dp,
    drawStopIndicator: (androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit)? = null,
) {
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        strokeCap = strokeCap,
        gapSize = gapSize,
        drawStopIndicator =
            drawStopIndicator ?: {
                androidx.compose.material3.ProgressIndicatorDefaults
                    .drawStopIndicator(this, 4.dp, color, strokeCap)
            },
    )
}
