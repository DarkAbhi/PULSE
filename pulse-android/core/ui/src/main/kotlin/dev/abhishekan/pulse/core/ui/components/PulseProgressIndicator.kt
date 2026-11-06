package dev.abhishekan.pulse.core.ui.components

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.ui.PulseTheme

/** Labeled, determinate progress using the same theme tokens as other Pulse controls. */
@Composable
fun PulseProgressIndicator(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.semantics { contentDescription = label },
        color = PulseTheme.colors.action,
        trackColor = PulseTheme.colors.surfaceStrong,
    )
}
