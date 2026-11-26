package dev.abhishekan.pulse.feature.garage.detail

import androidx.annotation.StringRes
import dev.abhishekan.pulse.feature.garage.R

internal enum class GarageTab(
    @param:StringRes val label: Int,
) {
    Overview(
        R.string.pulse_garage_overview,
    ),
    Fuel(
        R.string.pulse_garage_fuel,
    ),
    Care(R.string.pulse_garage_maintenance),
    Air(R.string.pulse_garage_tires_air),
}
