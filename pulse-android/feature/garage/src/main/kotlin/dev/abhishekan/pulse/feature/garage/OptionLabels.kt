package dev.abhishekan.pulse.feature.garage

import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiText

internal fun garageOption(value: String): UiText = when (value) {
    "petrol" -> uiText(R.string.pulse_garage_option_petrol)
    "diesel" -> uiText(R.string.pulse_garage_option_diesel)
    "lpg" -> uiText(R.string.pulse_garage_option_lpg)
    "cng" -> uiText(R.string.pulse_garage_option_cng)
    "electric" -> uiText(R.string.pulse_garage_option_electric)
    "full" -> uiText(R.string.pulse_garage_option_full)
    "partial" -> uiText(R.string.pulse_garage_option_partial)
    "missed" -> uiText(R.string.pulse_garage_option_missed)
    "service" -> uiText(R.string.pulse_garage_option_service)
    "repair" -> uiText(R.string.pulse_garage_option_repair)
    "insurance" -> uiText(R.string.pulse_garage_option_insurance)
    "washing" -> uiText(R.string.pulse_garage_option_washing)
    "tyres" -> uiText(R.string.pulse_garage_option_tyres)
    else -> UiText.Literal(value)
}
