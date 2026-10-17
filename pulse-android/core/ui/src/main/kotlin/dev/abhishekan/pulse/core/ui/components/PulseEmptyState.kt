package dev.abhishekan.pulse.core.ui.components


import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseEmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
        PulseText(title, style = PulseTextStyle.Title)
        PulseText(message)
    }
}

@Composable
fun PulseEmptyState(title: UiText, message: UiText, modifier: Modifier = Modifier) = PulseEmptyState(title.asString(), message.asString(), modifier)
