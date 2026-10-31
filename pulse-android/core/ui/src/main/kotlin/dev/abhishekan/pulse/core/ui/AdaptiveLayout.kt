package dev.abhishekan.pulse.core.ui
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Optional extra content spacing; the app shell already reserves system bars and navigation.
val LocalPulseContentPadding = staticCompositionLocalOf { PaddingValues(0.dp) }

enum class PulseWindowSize { Compact, Medium, Expanded }

val PulseWindowSize.pagePadding: Dp
    get() = if (this == PulseWindowSize.Compact) PulseTokens.medium else PulseTokens.large

fun pulseWindowSize(width: Dp): PulseWindowSize =
    when {
        width < 600.dp -> PulseWindowSize.Compact
        width < 840.dp -> PulseWindowSize.Medium
        else -> PulseWindowSize.Expanded
    }

/** Measures this surface, including split-screen and embedded panes. */
@Composable
fun PulseAdaptiveLayout(
    modifier: Modifier = Modifier,
    content: @Composable (PulseWindowSize) -> Unit,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.TopCenter) {
        content(pulseWindowSize(maxWidth))
    }
}
