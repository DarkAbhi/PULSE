package dev.abhishekan.pulse.core.ui.components


import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseLoadingState(label: String, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        CircularProgressIndicator()
        PulseText(label)
    }
}

@Composable
fun PulseLoadingState(label: UiText, modifier: Modifier = Modifier) = PulseLoadingState(label.asString(), modifier)
