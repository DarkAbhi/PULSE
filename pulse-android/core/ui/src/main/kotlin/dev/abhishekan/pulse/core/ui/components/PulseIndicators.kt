package dev.abhishekan.pulse.core.ui.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
) {
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = PulseTheme.colors.action,
        trackColor = PulseTheme.colors.surfaceMuted,
    )
}
