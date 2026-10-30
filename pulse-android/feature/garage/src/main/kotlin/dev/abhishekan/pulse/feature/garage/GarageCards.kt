package dev.abhishekan.pulse.feature.garage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.pulseDescription
import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeFormatter

internal fun decimal(v: BigDecimal?) = v?.stripTrailingZeros()?.toPlainString() ?: "—"

internal fun garageDate(v: Instant): String = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm").withZone(UserTimeZone.zone).format(v)

@Composable
internal fun GarageActions(
    label: String,
    enabled: Boolean,
    edit: () -> Unit,
    delete: () -> Unit,
) {
    // Stack actions so large text and narrow windows retain complete labels and touch targets.
    PulseButton(
        stringResource(R.string.pulse_garage_edit),
        edit,
        Modifier.pulseDescription(stringResource(R.string.pulse_garage_edit_1_s, label)),
        enabled,
        style = PulseButtonStyle.Text,
    )
    PulseButton(
        stringResource(R.string.pulse_garage_delete),
        delete,
        Modifier.pulseDescription(stringResource(R.string.pulse_garage_delete_1_s, label)),
        enabled,
        style = PulseButtonStyle.Text,
    )
}

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

@Composable
internal fun MaintenanceCards(
    data: VehicleHistory,
    vehicle: Long,
    enabled: Boolean,
    busy: Boolean,
    edit: (GarageDraft) -> Unit,
    remove: (GarageChange.Delete, UiText) -> Unit,
    upload: (Long) -> Unit,
    open: (Long, GarageReceipt) -> Unit,
    download: (Long, GarageReceipt) -> Unit,
    uploadRecord: Long?,
    progress: Float?,
) {
    if (data.maintenance.isEmpty()) {
        PulseEmptyState(
            stringResource(R.string.pulse_garage_no_maintenance_yet),
            stringResource(
                R.string.pulse_garage_keep_services_repairs_insurance_washes_and_tyres_together,
            ),
        )
    }
    data.maintenance.forEach { item ->
        MaintenanceHistoryCard(item, vehicle, enabled, busy, edit, remove, upload, open, download, uploadRecord, progress)
    }
}
