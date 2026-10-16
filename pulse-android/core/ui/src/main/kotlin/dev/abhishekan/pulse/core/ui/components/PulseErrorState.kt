package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
        PulseText("Couldn’t load this section", style = PulseTextStyle.Title)
        PulseText(message)
        PulseTextButton("Try again", onRetry)
    }
}
