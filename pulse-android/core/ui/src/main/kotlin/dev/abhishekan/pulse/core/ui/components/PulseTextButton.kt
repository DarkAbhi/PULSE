package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(onClick, modifier.defaultMinSize(minHeight = PulseTokens.target), enabled = enabled) { PulseText(text, style = PulseTextStyle.Label) }
}
