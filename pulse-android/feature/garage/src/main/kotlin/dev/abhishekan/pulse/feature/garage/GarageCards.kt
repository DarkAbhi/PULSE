package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun decimal(v: BigDecimal?) = v?.stripTrailingZeros()?.toPlainString() ?: "—"
internal fun garageDate(v: Instant): String = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm").withZone(ZoneId.systemDefault()).format(v)
@Composable
internal fun GarageActions(label: String, enabled: Boolean, edit: () -> Unit, delete: () -> Unit) {
    // Stack actions so large text and narrow windows retain complete labels and touch targets.
    PulseTextButton("Edit", edit, Modifier.semantics { contentDescription = "Edit $label" }, enabled)
    PulseTextButton("Delete", delete, Modifier.semantics { contentDescription = "Delete $label" }, enabled)
}
@Composable
internal fun PressureCard(pressure: TirePressure, enabled: Boolean, edit: () -> Unit, fill: () -> Unit) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText("A little air, a smoother ride", style = PulseTextStyle.Title)
        PulseText("Target tire pressure · PSI", style = PulseTextStyle.Label)
        PulseText("Solo · Front ${decimal(pressure.frontSolo)} · Rear ${decimal(pressure.rearSolo)}")
        PulseText("Pillion · Front ${decimal(pressure.frontPillion)} · Rear ${decimal(pressure.rearPillion)}")
        PulseTextButton("Set tire pressure", edit, enabled = enabled)
        PulseButton("Record air fill now", fill, enabled = enabled)
        PulseText("Air fills are recorded at the server’s current time.", style = PulseTextStyle.Label)
    }
}
@Composable
internal fun EfficiencyCards(data: VehicleHistory) {
    if (data.efficiency.isEmpty()) PulseEmptyState("Your story starts here", "Add fuel fill-ups to build an efficiency history. A complete full-to-full interval is needed for mileage.")
    data.efficiency.forEach { (type, stat) -> PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
        PulseText("${type.replaceFirstChar(Char::uppercase)} · your efficiency", style = PulseTextStyle.Title)
        PulseText(stat.average?.let { "${decimal(it)} km/L" } ?: "Not enough history", style = if (stat.average == null) PulseTextStyle.Title else PulseTextStyle.Metric)
        if (stat.average == null) PulseText("Record another full fill after a full tank to complete an interval. Missed fills reset the baseline.")
        PulseText("Last ${decimal(stat.last)} · Best ${decimal(stat.max)} · Lowest ${decimal(stat.min)} km/L")
        PulseText("Recorded volume · ${decimal(stat.totalVolume)} L")
        PulseText("Recorded cost · ₹${decimal(stat.totalCost)}")
    } }
}
@Composable
internal fun FuelCards(data: VehicleHistory, vehicle: Long, enabled: Boolean, edit: (GarageDraft) -> Unit, remove: (GarageChange.Delete, String) -> Unit) {
    if (data.fuel.isEmpty()) PulseEmptyState("No fuel fill-ups", "Record fuel, odometer and cost to start your history.")
    data.fuel.forEach { item -> PulseCard(Modifier.fillMaxWidth().testTag("garage-fuel-${item.id}")) {
        val label = "fuel fill-up ${item.id}"
        PulseText(garageDate(item.date), style = PulseTextStyle.Title)
        PulseText("${decimal(item.odometer)} km", style = PulseTextStyle.Metric)
        item.station?.let { PulseText(it) }; item.notes?.let { PulseText(it) }
        item.items.forEach { tank -> PulseText("${tank.type.replaceFirstChar(Char::uppercase)} · ${tank.fill} · ${decimal(tank.quantity)} L\n₹${decimal(tank.price)}/L · ₹${decimal(tank.total)} total") }
        GarageActions(label, enabled, { edit(item.draft(vehicle)) }, { remove(GarageChange.Delete(GarageRecord.Fuel, vehicle, item.id), "Permanently remove this fill-up and all of its fuel items? Efficiency summaries will be recalculated by the server.") })
    } }
}
@Composable
internal fun MaintenanceCards(data: VehicleHistory, vehicle: Long, enabled: Boolean, busy: Boolean, edit: (GarageDraft) -> Unit,
    remove: (GarageChange.Delete, String) -> Unit, upload: (Long) -> Unit, open: (Long, GarageReceipt) -> Unit, download: (Long, GarageReceipt) -> Unit, uploadRecord: Long?, progress: Float?) {
    if (data.maintenance.isEmpty()) PulseEmptyState("No maintenance yet", "Keep services, repairs, insurance, washes and tyres together.")
    data.maintenance.forEach { item -> PulseCard(Modifier.fillMaxWidth().testTag("garage-care-${item.id}")) {
        PulseText(item.title, style = PulseTextStyle.Title)
        PulseText("₹${decimal(item.amount)}", style = PulseTextStyle.Metric)
        PulseText("${item.category.replaceFirstChar(Char::uppercase)} · ${garageDate(item.date)}")
        item.odometer?.let { PulseText("${decimal(it)} km") }; item.provider?.let { PulseText(it) }; item.notes?.let { PulseText(it) }
        GarageActions(item.title, enabled, { edit(item.draft(vehicle)) }, { remove(GarageChange.Delete(GarageRecord.Maintenance, vehicle, item.id), "Delete “${item.title}”? This permanently removes the maintenance record, receipt metadata and its hosted receipt files. If storage cleanup fails, the server keeps the record; some files may already be removed.") })
        PulseText("Receipts", Modifier.semantics { heading() }, PulseTextStyle.Label)
        if (item.receipts.isEmpty()) PulseText("No receipts attached.")
        item.receipts.forEach { receipt ->
            PulseText("${receipt.name} · ${receipt.size} bytes")
            PulseTextButton("Open ${receipt.name}", { open(item.id, receipt) }, enabled = !busy)
            PulseTextButton("Download ${receipt.name}", { download(item.id, receipt) }, enabled = !busy)
            PulseTextButton("Remove ${receipt.name}", { remove(GarageChange.Delete(GarageRecord.Receipt, vehicle, receipt.id, item.id), "Permanently remove “${receipt.name}” from server storage and this maintenance record? The maintenance record stays.") }, enabled = enabled)
        }
        PulseTextButton("Attach receipt to ${item.title}", { upload(item.id) }, enabled = enabled)
        if (uploadRecord == item.id && progress != null) PulseTransferProgress(progress)
        PulseText("Up to 10 MB per file. Your saved record is kept if an upload fails.", style = PulseTextStyle.Label)
    } }
}
