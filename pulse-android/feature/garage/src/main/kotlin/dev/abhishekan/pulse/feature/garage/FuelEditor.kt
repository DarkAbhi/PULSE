package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.domain.model.garageFuelTypes
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCheckbox
import dev.abhishekan.pulse.core.ui.components.PulseDatePickerDialog
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTimePickerDialog
import java.time.LocalDate
import java.time.LocalTime

@Composable
internal fun FuelEditor(
    draft: GarageDraft,
    busy: Boolean,
    error: UiText?,
    change: (GarageDraft) -> Unit,
) {
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var timePicker by rememberSaveable { mutableStateOf(false) }
    var notesExpanded by rememberSaveable { mutableStateOf(draft["notes"].isNotBlank()) }
    val focus = LocalFocusManager.current
    val enabled = !busy

    @Composable fun field(
        key: String,
        label: String,
        numeric: Boolean = false,
    ) {
        PulseTextField(
            draft[key],
            { change(draft.set(key, it)) },
            label,
            Modifier.fillMaxWidth(),
            enabled,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }),
        )
    }
    Column(
        Modifier.widthIn(max = 760.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
    ) {
        PulseText(
            stringResource(if (draft.id == null) R.string.pulse_garage_add_fuel_fill_up else R.string.pulse_garage_edit_fuel),
            Modifier.semantics { heading() },
            PulseTextStyle.Display,
        )
        PulseText(stringResource(R.string.pulse_garage_fuel_subtitle), color = PulseTheme.colors.textSecondary)
        PulseCard(Modifier.fillMaxWidth()) {
            FuelSectionTitle(stringResource(R.string.pulse_garage_trip_details))
            BoxWithConstraints {
                val date: @Composable (Modifier) -> Unit = { modifier ->
                    PulseButton(stringResource(R.string.pulse_garage_date_1_s, draft["date"]), {
                        datePicker = true
                    }, modifier, enabled, style = PulseButtonStyle.Text)
                }
                val time: @Composable (Modifier) -> Unit = { modifier ->
                    PulseButton(stringResource(R.string.pulse_garage_time_1_s, draft["time"]), {
                        timePicker = true
                    }, modifier, enabled, style = PulseButtonStyle.Text)
                }
                if (maxWidth / LocalDensity.current.fontScale >= 320.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                        date(Modifier.weight(1f).background(PulseTheme.colors.surfaceMuted, PulseTokens.controlShape))
                        time(Modifier.weight(1f).background(PulseTheme.colors.surfaceMuted, PulseTokens.controlShape))
                    }
                } else {
                    Column {
                        date(Modifier.fillMaxWidth())
                        time(Modifier.fillMaxWidth())
                    }
                }
            }
            field("odometer", stringResource(R.string.pulse_garage_odometer_km), true)
            field("station", stringResource(R.string.pulse_garage_station_vendor_optional))
        }
        val tanks = draft["tanks"].toIntOrNull() ?: 1
        repeat(tanks) { tank ->
            PulseCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        PulseText(
                            stringResource(R.string.pulse_garage_tank_details),
                            style = PulseTextStyle.Label,
                            color = PulseTheme.colors.action,
                        )
                        FuelSectionTitle(stringResource(R.string.pulse_garage_tank_1_s, tank + 1))
                    }
                    if (tank == 1) {
                        PulseButton(
                            stringResource(R.string.pulse_garage_remove_second_tank),
                            { change(draft.set("tanks", "1")) },
                            enabled = enabled,
                            style = PulseButtonStyle.Text,
                        )
                    }
                }
                GarageChoice(
                    stringResource(R.string.pulse_garage_fuel_type_tank_1_s, tank + 1),
                    draft["type$tank"],
                    garageFuelTypes,
                    { change(draft.set("type$tank", it)) },
                    enabled,
                )
                FuelFillSelector(draft, tank, enabled, change)
                PulseText(
                    stringResource(R.string.pulse_garage_enter_any_two_values_pulse_calculates_the_remaining_value),
                    style = PulseTextStyle.Label,
                    color = PulseTheme.colors.textSecondary,
                )
                BoxWithConstraints {
                    val quantity: @Composable () -> Unit = {
                        field(
                            "quantity$tank",
                            stringResource(
                                R.string.pulse_garage_quantity_l_tank_1_s,
                                tank + 1,
                            ),
                            true,
                        )
                    }
                    val price: @Composable () -> Unit = {
                        field(
                            "price$tank",
                            stringResource(
                                R.string.pulse_garage_price_per_litre_tank_1_s,
                                UserCurrency
                                    .symbol(),
                                tank + 1,
                            ),
                            true,
                        )
                    }
                    val total: @Composable () -> Unit = {
                        field(
                            "total$tank",
                            stringResource(
                                R.string.pulse_garage_total_cost_tank_1_s,
                                UserCurrency
                                    .symbol(),
                                tank + 1,
                            ),
                            true,
                        )
                    }
                    if (maxWidth / LocalDensity.current.fontScale >= 600.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                            Column(Modifier.weight(1f)) { quantity() }
                            Column(Modifier.weight(1f)) { price() }
                            Column(Modifier.weight(1f)) { total() }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                            quantity()
                            price()
                            total()
                        }
                    }
                }
                if (draft["type$tank"] in listOf("cng", "electric")) {
                    PulseText(
                        stringResource(R.string.pulse_garage_the_current_api_records_all_fuel_quantities_in_l),
                        style = PulseTextStyle.Label,
                    )
                }
            }
        }
        PulseCard(Modifier.fillMaxWidth()) {
            FuelSectionTitle(stringResource(R.string.pulse_garage_additional_information))
            if (tanks < 2) {
                PulseButton(
                    stringResource(R.string.pulse_garage_add_second_fuel_tank),
                    {
                        change(
                            draft
                                .set(
                                    "tanks",
                                    "2",
                                ).set("type1", draft["type1"].ifBlank { "petrol" })
                                .set("fill1", draft["fill1"].ifBlank { "full" }),
                        )
                    },
                    Modifier.fillMaxWidth(),
                    enabled,
                    style = PulseButtonStyle.Text,
                )
            }
            PulseButton(
                stringResource(if (notesExpanded) R.string.pulse_garage_hide_notes else R.string.pulse_garage_notes_optional),
                { notesExpanded = !notesExpanded },
                enabled = enabled,
                style = PulseButtonStyle.Text,
            )
            if (notesExpanded) field("notes", stringResource(R.string.pulse_garage_notes_optional))
        }
        error?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = PulseTheme.colors.error) }
    }
    if (datePicker) {
        PulseDatePickerDialog(
            runCatching { LocalDate.parse(draft["date"]) }.getOrDefault(LocalDate.now(UserTimeZone.zone)),
            {
                change(draft.set("date", it.toString()))
                datePicker = false
            },
            { datePicker = false },
        )
    }
    if (timePicker) {
        PulseTimePickerDialog(
            runCatching { LocalTime.parse(draft["time"]) }.getOrDefault(LocalTime.MIDNIGHT),
            {
                change(draft.set("time", it.toString()))
                timePicker = false
            },
            { timePicker = false },
        )
    }
}

@Composable
private fun FuelSectionTitle(text: String) {
    PulseText(text, Modifier.semantics { heading() }, PulseTextStyle.Title)
}

@Composable
private fun FuelFillSelector(
    draft: GarageDraft,
    tank: Int,
    enabled: Boolean,
    change: (GarageDraft) -> Unit,
) {
    var help by rememberSaveable { mutableStateOf(false) }
    val fill = draft["fill$tank"]
    PulseText(stringResource(R.string.pulse_garage_fill_type_tank_1_s, tank + 1), style = PulseTextStyle.Label)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(
                PulseTokens.controlShape,
            ).background(PulseTheme.colors.surfaceMuted)
            .padding(PulseTokens.tiny)
            .selectableGroup(),
    ) {
        listOf("partial", "full").forEach { value ->
            val selected = fill == value
            PulseRadioButton(
                selected = selected,
                onClick = { change(draft.set("fill$tank", value)) },
                label = garageOption(value),
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
            )
        }
    }
    PulseCheckbox(fill == "missed", {
        change(draft.set("fill$tank", if (it) "missed" else "full"))
    }, stringResource(R.string.pulse_garage_missed_fill), enabled = enabled)
    PulseButton(stringResource(R.string.pulse_garage_fill_help), { help = !help }, enabled = enabled, style = PulseButtonStyle.Text)
    if (help) {
        PulseText(
            stringResource(
                when (fill) {
                    "partial" -> R.string.pulse_garage_partial_help
                    "missed" -> R.string.pulse_garage_missed_help
                    else -> R.string.pulse_garage_full_help
                },
            ),
            style = PulseTextStyle.Label,
            color = PulseTheme.colors.textSecondary,
        )
    }
}

@Composable
internal fun FuelEditorActions(
    busy: Boolean,
    blocked: Boolean,
    save: () -> Unit,
    cancel: () -> Unit,
) {
    val focus = LocalFocusManager.current
    Column(
        Modifier
            .widthIn(max = 760.dp)
            .fillMaxWidth()
            .background(PulseTheme.colors.background)
            .padding(vertical = PulseTokens.small),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PulseButton(
            stringResource(R.string.pulse_garage_save_fuel),
            {
                focus.clearFocus()
                save()
            },
            Modifier.fillMaxWidth(),
            enabled =
                !busy && !blocked,
            loading = busy,
        )
        PulseButton(stringResource(R.string.pulse_garage_cancel_editing), {
            focus.clearFocus()
            cancel()
        }, enabled = !busy, style = PulseButtonStyle.Text)
    }
}
