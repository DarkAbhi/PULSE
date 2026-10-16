package dev.abhishekan.pulse.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class PulseWindowSize { Compact, Medium, Expanded }

fun pulseWindowSize(width: Dp): PulseWindowSize = when {
    width < 600.dp -> PulseWindowSize.Compact
    width < 840.dp -> PulseWindowSize.Medium
    else -> PulseWindowSize.Expanded
}

/** Measures this surface, including split-screen and embedded panes. */
@Composable
fun PulseAdaptiveLayout(modifier: Modifier = Modifier, content: @Composable (PulseWindowSize) -> Unit) {
    BoxWithConstraints(modifier, contentAlignment = androidx.compose.ui.Alignment.TopCenter) { content(pulseWindowSize(maxWidth)) }
}
