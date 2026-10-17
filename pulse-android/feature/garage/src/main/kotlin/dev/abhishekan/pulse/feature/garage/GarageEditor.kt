package dev.abhishekan.pulse.feature.garage

import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.garage.R

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
internal fun GarageEditor(draft: GarageDraft, busy: Boolean, blocked: Boolean, error: UiText?, change: (GarageDraft) -> Unit, save: () -> Unit, cancel: () -> Unit) {
    var datePicker by remember { mutableStateOf(false) }
    var timePicker by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val enabled = !busy
    @Composable fun field(key: String, label: UiText, numeric: Boolean = false) {
        PulseTextField(draft[key], { change(draft.set(key, it)) }, label, Modifier.fillMaxWidth(), enabled,
            keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }))
    }
    @Composable fun choice(key: String, label: UiText, options: List<String>) {
        GarageChoice(label, draft[key], options, { change(draft.set(key, it)) }, enabled)
    }
    PulseCard(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth()) {
        PulseText(draft.kind.editorLabel(if (draft.id == null && draft.kind != GarageForm.Pressure) EditorAction.Add else EditorAction.Edit), style = PulseTextStyle.Title)
        when(draft.kind) {
            GarageForm.Vehicle -> { field("name", uiText(R.string.pulse_garage_vehicle_name)); PulseCheckbox(draft["active"] != "false", { change(draft.set("active", it.toString())) }, uiText(R.string.pulse_garage_active_vehicle), enabled = enabled) }
            GarageForm.Pressure -> PulseText(uiText(R.string.pulse_garage_target_tire_pressures_in_psi_leave_a_field_empty))
            GarageForm.Care -> { choice("category", uiText(R.string.pulse_garage_maintenance_category), garageCategories); field("title", uiText(R.string.pulse_garage_title)); field("amount", uiText(R.string.pulse_garage_amount), true) }
            GarageForm.Fuel -> { field("odometer", uiText(R.string.pulse_garage_odometer_km), true) }
        }
        if (draft.kind in listOf(GarageForm.Vehicle, GarageForm.Pressure)) {
            if (draft.kind == GarageForm.Vehicle) PulseText(uiText(R.string.pulse_garage_optional_target_tire_pressures_psi))
            field("frontSolo", uiText(R.string.pulse_garage_front_solo_psi), true); field("rearSolo", uiText(R.string.pulse_garage_rear_solo_psi), true)
            field("frontPillion", uiText(R.string.pulse_garage_front_pillion_psi), true); field("rearPillion", uiText(R.string.pulse_garage_rear_pillion_psi), true)
        }
        if (draft.kind in listOf(GarageForm.Fuel, GarageForm.Care)) {
            PulseTextButton(uiText(R.string.pulse_garage_date_1_s , draft["date"]), { datePicker = true }, enabled = enabled)
            PulseTextButton(uiText(R.string.pulse_garage_time_1_s , draft["time"]), { timePicker = true }, enabled = enabled)
            PulseText(uiText(R.string.pulse_garage_date_and_time_use_1_s , draft["zone"].ifBlank { ZoneId.systemDefault().id }), style = PulseTextStyle.Label)
        }
        if (draft.kind == GarageForm.Fuel) {
            field("station", uiText(R.string.pulse_garage_station_vendor_optional))
            val tanks = draft["tanks"].toIntOrNull() ?: 1
            repeat(tanks) { i ->
                PulseText(uiText(R.string.pulse_garage_tank_1_s , i + 1), Modifier.semantics { heading() }, PulseTextStyle.Title)
                choice("type$i", uiText(R.string.pulse_garage_fuel_type_tank_1_s , i + 1), garageFuelTypes)
                choice("fill$i", uiText(R.string.pulse_garage_fill_type_tank_1_s , i + 1), garageFillTypes)
                PulseText(uiText(R.string.pulse_garage_enter_any_two_values_pulse_calculates_the_remaining_value))
                field("quantity$i", uiText(R.string.pulse_garage_quantity_l_tank_1_s , i + 1), true)
                field("price$i", uiText(R.string.pulse_garage_price_per_litre_tank_1_s , i + 1), true)
                field("total$i", uiText(R.string.pulse_garage_total_cost_tank_1_s , i + 1), true)
                if (draft["type$i"] in listOf("cng", "electric")) PulseText(uiText(R.string.pulse_garage_the_current_api_records_all_fuel_quantities_in_l), style = PulseTextStyle.Label)
                if (i == 1) PulseTextButton(uiText(R.string.pulse_garage_remove_second_tank), { change(draft.set("tanks", "1")) }, enabled = enabled)
            }
            if (tanks < 2) PulseTextButton(uiText(R.string.pulse_garage_add_second_fuel_tank), { change(draft.set("tanks", "2").copy(fields = draft.fields + mapOf("tanks" to "2", "type1" to "petrol", "fill1" to "full"))) }, enabled = enabled)
            PulseText(uiText(R.string.pulse_garage_a_missed_fill_resets_the_efficiency_baseline_partial_fills), style = PulseTextStyle.Label)
        }
        if (draft.kind == GarageForm.Care) { field("odometer", uiText(R.string.pulse_garage_odometer_km_optional), true); field("provider", uiText(R.string.pulse_garage_provider_optional)); PulseText(uiText(R.string.pulse_garage_save_this_record_first_then_attach_receipts_from_its)) }
        if (draft.kind in listOf(GarageForm.Fuel, GarageForm.Care)) field("notes", uiText(R.string.pulse_garage_notes_optional))
        error?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.error) }
        PulseButton(draft.kind.editorLabel(EditorAction.Save), { focus.clearFocus(); save() }, Modifier.fillMaxWidth(), enabled = !blocked && enabled, loading = busy)
        PulseTextButton(uiText(R.string.pulse_garage_cancel_editing), { focus.clearFocus(); cancel() }, enabled = enabled)
    }
    if (datePicker) PulseDatePickerDialog(runCatching { LocalDate.parse(draft["date"]) }.getOrDefault(LocalDate.now()),
        { change(draft.set("date", it.toString())); datePicker = false }, { datePicker = false })
    if (timePicker) PulseTimePickerDialog(runCatching { LocalTime.parse(draft["time"]) }.getOrDefault(LocalTime.MIDNIGHT),
        { change(draft.set("time", it.toString())); timePicker = false }, { timePicker = false })
}
@Composable
internal fun GarageChoice(label: UiText, value: String, options: List<String>, select: (String) -> Unit, enabled: Boolean) {
    PulseSelectionField(label, garageOption(value), options.map(::garageOption), { select(options[it]) }, Modifier.fillMaxWidth(), enabled)
}
