package dev.abhishekan.pulse.core.ui.components

import dev.abhishekan.pulse.core.ui.R

import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.core.ui.asString

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
        PulseText(uiText(R.string.pulse_ui_couldn_t_load_this_section), style = PulseTextStyle.Title)
        PulseText(message)
        PulseTextButton(uiText(R.string.pulse_ui_try_again), onRetry)
    }
}

@Composable
fun PulseErrorState(message: UiText, onRetry: () -> Unit, modifier: Modifier = Modifier) = PulseErrorState(message.asString(), onRetry, modifier)
