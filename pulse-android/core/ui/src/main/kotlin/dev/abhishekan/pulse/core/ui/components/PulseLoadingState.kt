package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseLoadingState(
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Polite
        },
        verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
    ) {
        CircularProgressIndicator(color = PulseTheme.colors.action, trackColor = PulseTheme.colors.surfaceStrong)
        PulseText(label)
    }
}
