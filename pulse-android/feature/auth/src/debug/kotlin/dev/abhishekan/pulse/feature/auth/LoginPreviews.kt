package dev.abhishekan.pulse.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.UiText

@Preview(name = "Phone", widthDp = 360, heightDp = 800)
@Preview(name = "Tablet", widthDp = 1024, heightDp = 900)
@Preview(name = "Landscape", widthDp = 800, heightDp = 360)
@Preview(name = "Large text", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun LoginLightPreview() {
    PulseTheme(false) { LoginScreen(LoginUiState(), {}, {}, {}) }
}

@Preview(name = "Dark error", widthDp = 360, heightDp = 800)
@Composable
private fun LoginDarkPreview() {
    PulseTheme(true) {
        LoginScreen(LoginUiState(error = UiText.Literal("Check your connection and try again.")), {
        }, {}, {})
    }
}
