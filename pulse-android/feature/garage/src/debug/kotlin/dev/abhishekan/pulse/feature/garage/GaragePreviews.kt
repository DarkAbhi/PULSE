package dev.abhishekan.pulse.feature.garage

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*

@Preview(name = "Garage phone", widthDp = 393, heightDp = 850)
@Preview(name = "Garage tablet dark", widthDp = 1024, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Garage large text", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun GaragePreview() {
    PulseTheme { Surface { PulseAdaptiveLayout { size ->
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(PulseTokens.medium), verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
            PulseText("Your garage", style = PulseTextStyle.Display)
            if (size == PulseWindowSize.Compact) PressureCard(TirePressure("28".toBigDecimal(), "32".toBigDecimal()), true, {}, {})
            else Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                Column(Modifier.weight(1f)) { PressureCard(TirePressure("28".toBigDecimal(), "32".toBigDecimal()), true, {}, {}) }
                Column(Modifier.weight(1f)) { PulseEmptyState("Your story starts here", "Add fuel fill-ups to build your efficiency history.") }
            }
            PulseTransferProgress(.45f)
            PulseErrorState("Receipt storage is unavailable. Your maintenance record is saved.", {})
        }
    } } }
}
