package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.*
import androidx.compose.ui.semantics.*
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*
import java.time.*

@Composable
internal fun GarageEditor(draft: GarageDraft, busy: Boolean, blocked: Boolean, error: String?, change: (GarageDraft) -> Unit, save: () -> Unit, cancel: () -> Unit) {
    var datePicker by remember { mutableStateOf(false) }
    var timePicker by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val enabled = !busy
    @Composable fun field(key: String, label: String, numeric: Boolean = false) {
        PulseTextField(draft[key], { change(draft.set(key, it)) }, label, Modifier.fillMaxWidth(), enabled,
            keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }))
    }
    @Composable fun choice(key: String, label: String, options: List<String>) {
        GarageChoice(label, draft[key], options, { change(draft.set(key, it)) }, enabled)
    }
    PulseCard(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth()) {
        val label = when(draft.kind) { GarageForm.Vehicle -> "vehicle"; GarageForm.Fuel -> "fuel fill-up"; GarageForm.Care -> "maintenance"; GarageForm.Pressure -> "tire pressure" }
        PulseText("${if (draft.id == null && draft.kind != GarageForm.Pressure) "Add" else "Edit"} $label", style = PulseTextStyle.Title)
        when(draft.kind) {
            GarageForm.Vehicle -> { field("name", "Vehicle name"); PulseCheckbox(draft["active"] != "false", { change(draft.set("active", it.toString())) }, "Active vehicle", enabled = enabled) }
            GarageForm.Pressure -> PulseText("Target tire pressures in PSI. Leave a field empty to clear its target.")
            GarageForm.Care -> { choice("category", "Maintenance category", garageCategories); field("title", "Title"); field("amount", "Amount (₹)", true) }
            GarageForm.Fuel -> { field("odometer", "Odometer (km)", true) }
        }
        if (draft.kind in listOf(GarageForm.Vehicle, GarageForm.Pressure)) {
            if (draft.kind == GarageForm.Vehicle) PulseText("Optional target tire pressures (PSI)")
            field("frontSolo", "Front · solo (PSI)", true); field("rearSolo", "Rear · solo (PSI)", true)
            field("frontPillion", "Front · pillion (PSI)", true); field("rearPillion", "Rear · pillion (PSI)", true)
        }
        if (draft.kind in listOf(GarageForm.Fuel, GarageForm.Care)) {
            PulseTextButton("Date: ${draft["date"]}", { datePicker = true }, enabled = enabled)
            PulseTextButton("Time: ${draft["time"]}", { timePicker = true }, enabled = enabled)
            PulseText("Date and time use ${draft["zone"].ifBlank { ZoneId.systemDefault().id }}.", style = PulseTextStyle.Label)
        }
        if (draft.kind == GarageForm.Fuel) {
            field("station", "Station / vendor (optional)")
            val tanks = draft["tanks"].toIntOrNull() ?: 1
            repeat(tanks) { i ->
                PulseText("Tank ${i + 1}", Modifier.semantics { heading() }, PulseTextStyle.Title)
                choice("type$i", "Fuel type · tank ${i + 1}", garageFuelTypes)
                choice("fill$i", "Fill type · tank ${i + 1}", garageFillTypes)
                PulseText("Enter any two values. Pulse calculates the remaining value on the server.")
                field("quantity$i", "Quantity (L) · tank ${i + 1}", true)
                field("price$i", "Price per litre (₹) · tank ${i + 1}", true)
                field("total$i", "Total cost (₹) · tank ${i + 1}", true)
                if (draft["type$i"] in listOf("cng", "electric")) PulseText("The current API records all fuel quantities in L, including this category. No unit conversion is applied.", style = PulseTextStyle.Label)
                if (i == 1) PulseTextButton("Remove second tank", { change(draft.set("tanks", "1")) }, enabled = enabled)
            }
            if (tanks < 2) PulseTextButton("Add second fuel tank", { change(draft.set("tanks", "2").copy(fields = draft.fields + mapOf("tanks" to "2", "type1" to "petrol", "fill1" to "full"))) }, enabled = enabled)
            PulseText("A missed fill resets the efficiency baseline. Partial fills count toward the next full-to-full interval.", style = PulseTextStyle.Label)
        }
        if (draft.kind == GarageForm.Care) { field("odometer", "Odometer (km, optional)", true); field("provider", "Provider (optional)"); PulseText("Save this record first, then attach receipts from its history card.") }
        if (draft.kind in listOf(GarageForm.Fuel, GarageForm.Care)) field("notes", "Notes (optional)")
        error?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.error) }
        PulseButton("Save $label", { focus.clearFocus(); save() }, Modifier.fillMaxWidth(), enabled = !blocked && enabled, loading = busy)
        PulseTextButton("Cancel editing", { focus.clearFocus(); cancel() }, enabled = enabled)
    }
    if (datePicker) PulseDatePickerDialog(runCatching { LocalDate.parse(draft["date"]) }.getOrDefault(LocalDate.now()),
        { change(draft.set("date", it.toString())); datePicker = false }, { datePicker = false })
    if (timePicker) PulseTimePickerDialog(runCatching { LocalTime.parse(draft["time"]) }.getOrDefault(LocalTime.MIDNIGHT),
        { change(draft.set("time", it.toString())); timePicker = false }, { timePicker = false })
}
@Composable
internal fun GarageChoice(label: String, value: String, options: List<String>, select: (String) -> Unit, enabled: Boolean) {
    PulseSelectionField(label, value.replaceFirstChar(Char::uppercase), options.map { it.replaceFirstChar(Char::uppercase) }, { select(options[it]) }, Modifier.fillMaxWidth(), enabled)
}
