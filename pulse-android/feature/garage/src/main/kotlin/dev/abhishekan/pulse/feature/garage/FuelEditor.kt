package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
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
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseDatePickerDialog
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseFilterChip
import dev.abhishekan.pulse.core.ui.components.PulseSelectionField
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTimePickerDialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal val fuelFormWidth = 720.dp
private val fillOptions = listOf("full", "partial", "missed")

@Composable
internal fun FuelEditor(
    draft: GarageDraft,
    busy: Boolean,
    error: UiText?,
    change: (GarageDraft) -> Unit,
) {
    val enabled = !busy
    val tanks = draft["tanks"].toIntOrNull() ?: 1
    val focus = LocalFocusManager.current

    Column(
        Modifier.widthIn(max = fuelFormWidth).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
    ) {
        FuelTripDetails(draft, enabled, change)
        repeat(tanks) { tank ->
            FuelTankDetails(draft, tank, tanks, enabled, change)
        }
        if (tanks == 1) {
            PulseButton(
                stringResource(R.string.pulse_garage_add_second_fuel_tank),
                {
                    focus.clearFocus()
                    change(
                        draft
                            .set("tanks", "2")
                            .set("type1", draft["type1"].ifBlank { "petrol" })
                            .set("fill1", draft["fill1"].ifBlank { "full" }),
                    )
                },
                Modifier.fillMaxWidth(),
                enabled = enabled,
                variant = PulseButtonVariant.Secondary,
                leadingIcon = { Icon(painterResource(R.drawable.garage_add), null, Modifier.size(20.dp)) },
            )
        }
        FuelSection(stringResource(R.string.pulse_garage_optional_details), R.drawable.garage_document) {
            FuelInput(
                draft["station"],
                { change(draft.set("station", it)) },
                stringResource(R.string.pulse_garage_station_vendor_optional),
                enabled,
            )
            FuelInput(
                draft["notes"],
                { change(draft.set("notes", it)) },
                stringResource(R.string.pulse_garage_notes_optional),
                enabled = enabled,
                multiline = true,
            )
        }
        error?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = PulseTheme.colors.error) }
    }
}

@Composable
private fun FuelTripDetails(
    draft: GarageDraft,
    enabled: Boolean,
    change: (GarageDraft) -> Unit,
) {
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var timePicker by rememberSaveable { mutableStateOf(false) }
    val date = runCatching { LocalDate.parse(draft["date"]) }.getOrNull()
    val time = runCatching { LocalTime.parse(draft["time"]) }.getOrNull()
    FuelSection(stringResource(R.string.pulse_garage_trip_details), R.drawable.garage_odometer) {
        FuelInput(
            draft["odometer"],
            { change(draft.set("odometer", it)) },
            stringResource(R.string.pulse_garage_odometer_km),
            enabled,
            numeric = true,
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= 360.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)) 2 else 1
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
                maxItemsInEachRow = columns,
            ) {
                FuelPickerField(
                    stringResource(R.string.pulse_garage_fuel_date),
                    date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: draft["date"],
                    R.drawable.garage_calendar,
                    enabled,
                    Modifier.weight(1f),
                ) { datePicker = true }
                FuelPickerField(
                    stringResource(R.string.pulse_garage_fuel_time),
                    time?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) ?: draft["time"],
                    R.drawable.garage_clock,
                    enabled,
                    Modifier.weight(1f),
                ) { timePicker = true }
            }
        }
    }
    if (datePicker) {
        PulseDatePickerDialog(
            date ?: LocalDate.now(UserTimeZone.zone),
            {
                change(draft.set("date", it.toString()))
                datePicker = false
            },
            { datePicker = false },
        )
    }
    if (timePicker) {
        PulseTimePickerDialog(
            time ?: LocalTime.MIDNIGHT,
            {
                change(draft.set("time", it.toString()))
                timePicker = false
            },
            { timePicker = false },
        )
    }
}

@Composable
private fun FuelTankDetails(
    draft: GarageDraft,
    tank: Int,
    tanks: Int,
    enabled: Boolean,
    change: (GarageDraft) -> Unit,
) {
    val number = tank + 1
    val fuel = draft["type$tank"]
    val fill = draft["fill$tank"]
    val focus = LocalFocusManager.current
    val title =
        if (tanks == 1) {
            stringResource(R.string.pulse_garage_tank_details)
        } else {
            stringResource(R.string.pulse_garage_tank_1_s, number)
        }
    FuelSection(title, R.drawable.garage_option_fuel) {
        PulseSelectionField(
            label = stringResource(R.string.pulse_garage_fuel_type),
            selected = garageOption(fuel),
            options = garageFuelTypes.map { garageOption(it) },
            onSelect = { change(draft.set("type$tank", garageFuelTypes[it])) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            labelAbove = true,
        )
        FuelFillSelector(fill, enabled) { selection ->
            focus.clearFocus()
            if (selection != fill) change(draft.set("fill$tank", selection))
        }
        Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            FuelValues(draft, tank, enabled, change)
            if (fuel == "cng" || fuel == "electric") {
                PulseText(
                    stringResource(R.string.pulse_garage_the_current_api_records_all_fuel_quantities_in_l),
                    style = PulseTextStyle.Body2,
                    color = PulseTheme.colors.textSecondary,
                )
            }
        }
        if (tank == 1) {
            PulseButton(
                stringResource(R.string.pulse_garage_remove_second_tank),
                {
                    focus.clearFocus()
                    change(draft.set("tanks", "1"))
                },
                enabled = enabled,
                style = PulseButtonStyle.Text,
            )
        }
    }
}

@Composable
private fun FuelValues(
    draft: GarageDraft,
    tank: Int,
    enabled: Boolean,
    change: (GarageDraft) -> Unit,
) {
    val currency = UserCurrency.symbol()
    val number = tank + 1
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val readableWidth = maxWidth / LocalDensity.current.fontScale
        val columns =
            when {
                readableWidth >= PulseTokens.formColumnsWidth -> 3
                readableWidth >= 272.dp -> 2
                else -> 1
            }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
            maxItemsInEachRow = columns,
        ) {
            FuelInput(
                draft["quantity$tank"],
                { change(draft.setFuelValue(tank, "quantity", it)) },
                stringResource(R.string.pulse_garage_fuel_quantity),
                enabled,
                Modifier.weight(1f),
                numeric = true,
                accessibilityLabel = stringResource(R.string.pulse_garage_quantity_l_tank_1_s, number),
            )
            FuelInput(
                draft["price$tank"],
                { change(draft.setFuelValue(tank, "price", it)) },
                stringResource(R.string.pulse_garage_fuel_unit_price),
                enabled,
                Modifier.weight(1f),
                numeric = true,
                currency = currency,
                accessibilityLabel = stringResource(R.string.pulse_garage_price_per_litre_tank_1_s, currency, number),
            )
            FuelInput(
                draft["total$tank"],
                { change(draft.set("total$tank", it)) },
                stringResource(R.string.pulse_garage_fuel_total_cost),
                enabled,
                Modifier.weight(1f),
                numeric = true,
                currency = currency,
                accessibilityLabel = stringResource(R.string.pulse_garage_total_cost_tank_1_s, currency, number),
            )
        }
    }
}

@Composable
private fun FuelInput(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    multiline: Boolean = false,
    currency: String? = null,
    accessibilityLabel: String = label,
) {
    val focus = LocalFocusManager.current
    Column(modifier) {
        PulseTextField(
            value = value,
            onValueChange = onChange,
            label = label,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            labelAbove = true,
            accessibilityLabel = accessibilityLabel,
            leadingIcon = currency?.let { { PulseText(it, color = PulseTheme.colors.textSecondary) } },
            singleLine = !multiline,
            minLines = if (multiline) 2 else 1,
            maxLines = if (multiline) 5 else 1,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text,
                    imeAction =
                        if (numeric) {
                            ImeAction.Next
                        } else if (multiline) {
                            ImeAction.Default
                        } else {
                            ImeAction.Done
                        },
                ),
            keyboardActions =
                KeyboardActions(
                    onNext = { focus.moveFocus(FocusDirection.Next) },
                    onDone = { focus.clearFocus() },
                ),
        )
    }
}

@Composable
private fun FuelSection(
    title: String,
    icon: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = PulseTheme.colors
    Surface(
        Modifier.fillMaxWidth(),
        shape = PulseTokens.cardShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(
            Modifier.padding(PulseTokens.medium),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = PulseTheme.shapes.medium, color = colors.primarySoft) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Icon(painterResource(icon), null, Modifier.size(20.dp), tint = colors.onPrimarySoft)
                    }
                }
                PulseText(title, Modifier.semantics { heading() }, style = PulseTextStyle.Title3)
            }
            content()
        }
    }
}

@Composable
private fun FuelPickerField(
    label: String,
    value: String,
    icon: Int,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val focus = LocalFocusManager.current
    val description = stringResource(R.string.pulse_garage_fuel_picker_description, label, value)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
        PulseText(label, style = PulseTextStyle.Label)
        PulseButton(
            value,
            {
                focus.clearFocus()
                onClick()
            },
            Modifier.fillMaxWidth().semantics { contentDescription = description },
            enabled = enabled,
            variant = PulseButtonVariant.Secondary,
            leadingIcon = { Icon(painterResource(icon), null, Modifier.size(18.dp)) },
        )
    }
}

@Composable
private fun FuelFillSelector(
    fill: String,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    var help by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulseText(stringResource(R.string.pulse_garage_fuel_fill_type), Modifier.weight(1f), style = PulseTextStyle.Label)
            PulseButton(
                stringResource(R.string.pulse_garage_fill_help),
                {
                    focus.clearFocus()
                    help = true
                },
                enabled = enabled,
                style = PulseButtonStyle.Icon,
                icon = { Icon(painterResource(R.drawable.garage_info), null, Modifier.size(20.dp)) },
            )
        }
        FlowRow(
            Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
        ) {
            fillOptions.forEach { option ->
                PulseFilterChip(
                    label = if (option == "missed") stringResource(R.string.pulse_garage_missed_fill) else garageOption(option),
                    selected = fill == option,
                    onClick = { onSelect(option) },
                    enabled = enabled,
                )
            }
        }
    }
    if (help) {
        val message =
            when (fill) {
                "partial" -> R.string.pulse_garage_partial_help
                "missed" -> R.string.pulse_garage_missed_help
                else -> R.string.pulse_garage_full_help
            }
        PulseDialog(
            onDismissRequest = { help = false },
            confirmButton = {
                PulseButton(stringResource(android.R.string.ok), { help = false }, style = PulseButtonStyle.Text)
            },
            title = { PulseText(stringResource(R.string.pulse_garage_fill_help), style = PulseTextStyle.Title) },
            text = { PulseText(stringResource(message), Modifier.verticalScroll(rememberScrollState())) },
        )
    }
}
