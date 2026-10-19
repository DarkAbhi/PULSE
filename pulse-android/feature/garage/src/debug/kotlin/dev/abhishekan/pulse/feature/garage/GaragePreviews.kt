package dev.abhishekan.pulse.feature.garage
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.domain.model.TirePressure
import dev.abhishekan.pulse.core.domain.model.Vehicle
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTransferProgress

@Preview(name = "Garage phone", widthDp = 393, heightDp = 850)
@Preview(
    name = "Garage tablet dark",
    widthDp = 1024,
    heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(name = "Garage large text", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun GaragePreview() {
    PulseTheme {
        Surface {
            PulseAdaptiveLayout { size ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(
                            rememberScrollState(),
                        ).padding(PulseTokens.medium),
                    verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
                ) {
                    PulseText("Your garage", style = PulseTextStyle.Display)
                    if (size ==
                        PulseWindowSize.Compact
                    ) {
                        PressureCard(TirePressure("28".toBigDecimal(), "32".toBigDecimal()), true, {
                        }, {})
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                            Column(Modifier.weight(1f)) {
                                PressureCard(
                                    TirePressure(
                                        "28".toBigDecimal(),
                                        "32".toBigDecimal(),
                                    ),
                                    true,
                                    {
                                    },
                                    {},
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                PulseEmptyState(
                                    "Your story starts here",
                                    "Add fuel fill-ups to build your efficiency history.",
                                )
                            }
                        }
                    }
                    PulseTransferProgress(.45f)
                    PulseErrorState("Receipt storage is unavailable. Your maintenance record is saved.", {
                    })
                }
            }
        }
    }
}

@Preview(name = "Garage collection phone", widthDp = 393, heightDp = 850)
@Preview(name = "Garage collection tablet", widthDp = 1280, heightDp = 800)
@Preview(name = "Garage collection large text", widthDp = 360, heightDp = 900, fontScale = 2f)
@Preview(name = "Garage collection dark", widthDp = 393, heightDp = 850, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GarageCollectionPreview() {
    PulseTheme {
        Surface(color = PulseTheme.colors.background) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(PulseTokens.cardPadding),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.large),
            ) {
                GarageCollectionHeader(true) {}
                GarageVehicleGrid(
                    listOf(
                        Vehicle(1, "Himalayan 411", "two_wheeler", listOf("petrol")),
                        Vehicle(2, "Suzuki Access 125", "two_wheeler", listOf("petrol")),
                    ),
                    null,
                    true,
                    {
                    },
                )
            }
        }
    }
}

@Preview(name = "Fuel cards phone", widthDp = 393, heightDp = 850)
@Preview(name = "Fuel cards tablet", widthDp = 1024, heightDp = 900)
@Preview(name = "Fuel cards large text", widthDp = 360, heightDp = 850, fontScale = 2f)
@Preview(name = "Fuel cards dark", widthDp = 393, heightDp = 850, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FuelEditorPreview() {
    PulseTheme {
        Surface(color = PulseTheme.colors.background) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(PulseTokens.medium)) {
                    FuelEditor(GarageDraft.new(GarageForm.Fuel, 1), false, null, {})
                }
                FuelEditorActions(false, false, {}, {})
            }
        }
    }
}
