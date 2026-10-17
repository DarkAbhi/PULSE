package dev.abhishekan.pulse.feature.garage

import androidx.compose.ui.res.stringResource

@androidx.compose.runtime.Composable
internal fun garageOption(value: String): String = when (value) {
    "petrol" -> stringResource(R.string.pulse_garage_option_petrol)
    "diesel" -> stringResource(R.string.pulse_garage_option_diesel)
    "lpg" -> stringResource(R.string.pulse_garage_option_lpg)
    "cng" -> stringResource(R.string.pulse_garage_option_cng)
    "electric" -> stringResource(R.string.pulse_garage_option_electric)
    "full" -> stringResource(R.string.pulse_garage_option_full)
    "partial" -> stringResource(R.string.pulse_garage_option_partial)
    "missed" -> stringResource(R.string.pulse_garage_option_missed)
    "service" -> stringResource(R.string.pulse_garage_option_service)
    "repair" -> stringResource(R.string.pulse_garage_option_repair)
    "insurance" -> stringResource(R.string.pulse_garage_option_insurance)
    "washing" -> stringResource(R.string.pulse_garage_option_washing)
    "tyres" -> stringResource(R.string.pulse_garage_option_tyres)
    else -> value
}
