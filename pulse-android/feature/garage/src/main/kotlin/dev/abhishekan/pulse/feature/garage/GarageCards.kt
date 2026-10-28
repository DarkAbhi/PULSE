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
import dev.abhishekan.pulse.core.domain.model.TirePressure
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
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
    PulseTextButton(
        stringResource(R.string.pulse_garage_edit),
        edit,
        Modifier.pulseDescription(stringResource(R.string.pulse_garage_edit_1_s, label)),
        enabled,
    )
    PulseTextButton(
        stringResource(R.string.pulse_garage_delete),
        delete,
        Modifier.pulseDescription(stringResource(R.string.pulse_garage_delete_1_s, label)),
        enabled,
    )
}

@Composable
internal fun PressureCard(
    pressure: TirePressure,
    enabled: Boolean,
    edit: () -> Unit,
    fill: () -> Unit,
) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(
            stringResource(R.string.pulse_garage_a_little_air_a_smoother_ride),
            style = PulseTextStyle.Title,
        )
        PulseText(
            stringResource(R.string.pulse_garage_target_tire_pressure_psi),
            style = PulseTextStyle.Label,
        )
        PulseText(
            stringResource(
                R.string.pulse_garage_solo_front_1_s_rear_2_s,
                decimal(pressure.frontSolo),
                decimal(pressure.rearSolo),
            ),
        )
        PulseText(
            stringResource(
                R.string.pulse_garage_pillion_front_1_s_rear_2_s,
                decimal(pressure.frontPillion),
                decimal(pressure.rearPillion),
            ),
        )
        PulseTextButton(
            stringResource(R.string.pulse_garage_set_tire_pressure),
            edit,
            enabled = enabled,
        )
        PulseButton(
            stringResource(R.string.pulse_garage_record_air_fill_now),
            fill,
            enabled = enabled,
        )
        PulseText(
            stringResource(R.string.pulse_garage_air_fills_are_recorded_at_the_server_s_current),
            style = PulseTextStyle.Label,
        )
    }
}

@Composable
internal fun EfficiencyCards(data: VehicleHistory) {
    if (data.efficiency.isEmpty()) {
        PulseEmptyState(
            stringResource(R.string.pulse_garage_your_story_starts_here),
            stringResource(R.string.pulse_garage_add_fuel_fill_ups_to_build_an_efficiency_history),
        )
    }
    data.efficiency.forEach { (type, stat) ->
        PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
            PulseText(
                stringResource(R.string.pulse_garage_fuel_efficiency_title, garageOption(type)),
                style = PulseTextStyle.Title,
            )
            PulseText(
                stat.average?.let {
                    stringResource(R.string.pulse_garage_efficiency_km_per_litre, decimal(it))
                }
                    ?: stringResource(R.string.pulse_garage_not_enough_history),
                style =
                    if (stat.average ==
                        null
                    ) {
                        PulseTextStyle.Title
                    } else {
                        PulseTextStyle.Metric
                    },
            )
            if (stat.average ==
                null
            ) {
                PulseText(
                    stringResource(
                        R.string.pulse_garage_record_another_full_fill_after_a_full_tank_to,
                    ),
                )
            }
            PulseText(
                stringResource(
                    R.string.pulse_garage_last_1_s_best_2_s_lowest_3_s,
                    decimal(stat.last),
                    decimal(stat.max),
                    decimal(stat.min),
                ),
            )
            PulseText(
                stringResource(
                    R.string.pulse_garage_recorded_volume_1_s_l,
                    decimal(stat.totalVolume),
                ),
            )
            PulseText(
                stringResource(R.string.pulse_garage_recorded_cost_1_s, decimal(stat.totalCost)),
            )
        }
    }
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
    data.fuel.forEach { item ->
        PulseCard(Modifier.fillMaxWidth().testTag("garage-fuel-${item.id}")) {
            val label = stringResource(R.string.pulse_garage_fuel_fill_up_1_s, item.id)
            PulseText(garageDate(item.date), style = PulseTextStyle.Title)
            PulseText(
                stringResource(R.string.pulse_garage_distance_km, decimal(item.odometer)),
                style = PulseTextStyle.Metric,
            )
            item.station?.let { PulseText(it) }
            item.notes?.let { PulseText(it) }
            item.items.forEach { tank ->
                PulseText(
                    stringResource(
                        R.string.pulse_garage_fuel_tank_summary,
                        garageOption(tank.type),
                        garageOption(tank.fill),
                        decimal(tank.quantity),
                        decimal(tank.price),
                        decimal(tank.total),
                    ),
                )
            }
            GarageActions(label, enabled, {
                edit(item.draft(vehicle))
            }, {
                remove(
                    GarageChange.Delete(GarageRecord.Fuel, vehicle, item.id),
                    uiText(R.string.pulse_garage_permanently_remove_this_fill_up_and_all_of_its),
                )
            })
        }
    }
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
                stringResource(R.string.pulse_garage_formatted_value, decimal(item.amount)),
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
                PulseTextButton(stringResource(R.string.pulse_garage_open_1_s, receipt.name), {
                    open(item.id, receipt)
                }, enabled = !busy)
                PulseTextButton(stringResource(R.string.pulse_garage_download_1_s, receipt.name), {
                    download(item.id, receipt)
                }, enabled = !busy)
                PulseTextButton(stringResource(R.string.pulse_garage_remove_1_s, receipt.name), {
                    remove(
                        GarageChange.Delete(GarageRecord.Receipt, vehicle, receipt.id, item.id),
                        uiText(
                            R.string.pulse_garage_permanently_remove_1_s_from_server_storage_and_this,
                            receipt.name,
                        ),
                    )
                }, enabled = enabled)
            }
            PulseTextButton(
                stringResource(
                    R.string.pulse_garage_attach_receipt_to_1_s,
                    item.title,
                ),
                {
                    upload(item.id)
                },
                enabled = enabled,
            )
            if (uploadRecord == item.id && progress != null) PulseTransferProgress(progress)
            PulseText(
                stringResource(R.string.pulse_garage_up_to_10_mb_per_file_your_saved_record),
                style = PulseTextStyle.Label,
            )
        }
    }
}
