package dev.abhishekan.pulse.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Horizon
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.ui.PulseTheme
import java.math.BigDecimal

private val preview = HomeUiState(profile = Section.Content(Profile("Preview person")), workout = Section.Empty,
    horizon = Section.Content(Horizon("₹", BigDecimal("50000.00"), BigDecimal("32000.125"), BigDecimal("18000.00"))),
    notifications = Section.Empty)
@Preview(name = "Phone", widthDp = 360, heightDp = 800)
@Preview(name = "Tablet", widthDp = 1024, heightDp = 900)
@Preview(name = "Large text", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable private fun HomeLightPreview() { PulseTheme(false) { HomeScreen("preview", preview, {}, {}, {}, {}, {}, {}) } }
@Preview(name = "Dark partial failure", widthDp = 1024, heightDp = 900)
@Composable private fun HomeDarkPreview() {
    PulseTheme(true) { HomeScreen("preview", preview.copy(horizon = Section.Error(Failure.Connection)), {}, {}, {}, {}, {}, {}) }
}
