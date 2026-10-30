package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.domain.model.GarageRecord
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTransferProgress
import dev.abhishekan.pulse.core.ui.pulseDescription
import dev.abhishekan.pulse.core.ui.uiText
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
        PulseCard(Modifier.fillMaxWidth().testTag("garage-care-${item.id}")) {
            PulseText(item.title, style = PulseTextStyle.Title)
            PulseText(
                stringResource(
                    R.string.pulse_garage_formatted_value,
                    dev.abhishekan.pulse.core.domain.model.UserCurrency
                        .format(item.amount),
                ),
                style = PulseTextStyle.Metric,
            )
            PulseText(
                stringResource(
                    R.string.pulse_garage_maintenance_date,
                    garageOption(item.category),
                    garageDate(item.date),
                ),
            )
            item.odometer?.let {
                PulseText(stringResource(R.string.pulse_garage_distance_km, decimal(it)))
            }
            item.provider?.let { PulseText(it) }
            item.notes?.let { PulseText(it) }
            GarageActions(item.title, enabled, {
                edit(item.draft(vehicle))
            }, {
                remove(
                    GarageChange.Delete(GarageRecord.Maintenance, vehicle, item.id),
                    uiText(
                        R.string.pulse_garage_delete_1_s_this_permanently_removes_the_maintenance_record,
                        item.title,
                    ),
                )
            })
            PulseText(
                stringResource(R.string.pulse_garage_receipts),
                Modifier.semantics {
                    heading()
                },
                PulseTextStyle.Label,
            )
            if (item.receipts.isEmpty()) {
                PulseText(
                    stringResource(R.string.pulse_garage_no_receipts_attached),
                )
            }
            item.receipts.forEach { receipt ->
                PulseText(
                    pluralStringResource(
                        R.plurals.pulse_garage_receipt_size,
                        receipt.size.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                        receipt.name,
                        receipt.size,
                    ),
                )
                PulseButton(stringResource(R.string.pulse_garage_open_1_s, receipt.name), {
                    open(item.id, receipt)
                }, enabled = !busy, style = PulseButtonStyle.Text)
                PulseButton(stringResource(R.string.pulse_garage_download_1_s, receipt.name), {
                    download(item.id, receipt)
                }, enabled = !busy, style = PulseButtonStyle.Text)
                PulseButton(stringResource(R.string.pulse_garage_remove_1_s, receipt.name), {
                    remove(
                        GarageChange.Delete(GarageRecord.Receipt, vehicle, receipt.id, item.id),
                        uiText(
                            R.string.pulse_garage_permanently_remove_1_s_from_server_storage_and_this,
                            receipt.name,
                        ),
                    )
                }, enabled = enabled, style = PulseButtonStyle.Text)
            }
            PulseButton(
                stringResource(
                    R.string.pulse_garage_attach_receipt_to_1_s,
                    item.title,
                ),
                {
                    upload(item.id)
                },
                enabled = enabled,
                style = PulseButtonStyle.Text,
            )
            if (uploadRecord == item.id && progress != null) PulseTransferProgress(progress)
            PulseText(
                stringResource(R.string.pulse_garage_up_to_10_mb_per_file_your_saved_record),
                style = PulseTextStyle.Label,
            )
        }
    }
}
