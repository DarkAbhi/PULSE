package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
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
    var workQuery by rememberSaveable(vehicle) { mutableStateOf("") }
    PulseTextField(workQuery, { workQuery = it }, stringResource(R.string.pulse_garage_filter_work), Modifier.fillMaxWidth())
    val matches =
        data.maintenance.filter { record ->
            workQuery.isBlank() ||
                record.actions.any { action ->
                    val words = listOf(action.component, action.action, action.description).joinToString(" ").replace('_', ' ')
                    words.contains(workQuery.trim(), ignoreCase = true)
                }
        }
    if (matches.isEmpty() && workQuery.isNotBlank()) PulseText(stringResource(R.string.pulse_garage_no_matching_work))
    if (data.maintenance.isEmpty()) {
        PulseEmptyState(
            stringResource(R.string.pulse_garage_no_maintenance_yet),
            stringResource(
                R.string.pulse_garage_keep_services_repairs_insurance_washes_and_tyres_together,
            ),
        )
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 840.dp && LocalDensity.current.fontScale <= 1.3f) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
            matches.chunked(columns).forEach { entries ->
                Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                    entries.forEach { item ->
                        key(item.id) {
                            MaintenanceHistoryCard(
                                item,
                                vehicle,
                                enabled,
                                busy,
                                edit,
                                remove,
                                upload,
                                open,
                                download,
                                uploadRecord,
                                progress,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (entries.size < columns) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
