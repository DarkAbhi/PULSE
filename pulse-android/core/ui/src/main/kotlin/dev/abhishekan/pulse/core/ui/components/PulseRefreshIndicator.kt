package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abhishekan.pulse.core.ui.PulseTheme

@Composable
fun PulseRefreshIndicator() {
    LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth(),
        color = PulseTheme.colors.action,
        trackColor = PulseTheme.colors.surfaceStrong,
    )
}
