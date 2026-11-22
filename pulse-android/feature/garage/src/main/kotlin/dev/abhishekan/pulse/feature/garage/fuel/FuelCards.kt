package dev.abhishekan.pulse.feature.garage.fuel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.patterns.PulseEmptyState
import dev.abhishekan.pulse.feature.garage.R
import dev.abhishekan.pulse.feature.garage.editor.GarageDraft

@Composable
internal fun FuelCards(
    data: VehicleHistory,
    vehicle: Long,
    enabled: Boolean,
    edit: (GarageDraft) -> Unit,
    remove: (GarageChange.Delete, UiText) -> Unit,
) {
    if (data.fuel.isEmpty()) {
        PulseEmptyState(
            stringResource(R.string.pulse_garage_no_fuel_fill_ups),
            stringResource(
                R.string.pulse_garage_record_fuel_odometer_and_cost_to_start_your_history,
            ),
        )
    }
    FuelHistoryGrid(data.fuel, vehicle, enabled, edit, remove)
}
