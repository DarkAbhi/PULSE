package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
        PulseText(title, style = PulseTextStyle.Title)
        PulseText(message)
    }
}
