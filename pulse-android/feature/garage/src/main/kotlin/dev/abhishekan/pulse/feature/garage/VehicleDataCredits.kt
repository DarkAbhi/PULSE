package dev.abhishekan.pulse.feature.garage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseDialog

@Composable
internal fun VehicleDataCredits() {
    var showCredits by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    PulseButton(stringResource(R.string.pulse_garage_vehicle_data_credit), {
        uri.openUri("https://vehiclesdb.com")
    }, style = PulseButtonStyle.Text)
    PulseButton(stringResource(R.string.pulse_garage_data_sources), { showCredits = true }, style = PulseButtonStyle.Text)
    if (showCredits) {
        val resources = LocalContext.current.resources
        val notices =
            remember(resources) {
                resources
                    .openRawResource(R.raw.vehiclesdb_attribution)
                    .bufferedReader()
                    .use {
                        it.readText()
                    }.replace(Regex("<!--[\\s\\S]*?-->"), "")
                    .trim()
            }
        PulseDialog(
            stringResource(R.string.pulse_garage_data_sources),
            stringResource(R.string.pulse_garage_data_version) + "\n\n" + notices,
            stringResource(R.string.pulse_garage_close_credits),
            {
                showCredits =
                    false
            },
            { showCredits = false },
        )
    }
}
