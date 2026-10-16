package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false) {
    Button(onClick, modifier.defaultMinSize(minHeight = PulseTokens.target).semantics { if (loading) stateDescription = "Loading" },
        enabled = enabled && !loading, shape = PulseTokens.controlShape) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(PulseTokens.medium), color = LocalContentColor.current)
            Spacer(Modifier.width(PulseTokens.small))
        }
        PulseText(text, style = PulseTextStyle.Label)
    }
}
