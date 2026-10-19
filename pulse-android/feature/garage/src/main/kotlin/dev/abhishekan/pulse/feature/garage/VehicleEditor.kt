package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.garageFuelTypes
import dev.abhishekan.pulse.core.domain.model.garageVehicleTypes
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCheckbox
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

@Composable
internal fun VehicleEditor(
    draft: GarageDraft,
    busy: Boolean,
    blocked: Boolean,
    error: UiText?,
    change: (GarageDraft) -> Unit,
    save: () -> Unit,
    cancel: () -> Unit,
) {
    val colors = PulseTheme.colors
    val focus = LocalFocusManager.current
    BoxWithConstraints(Modifier.widthIn(max = 1080.dp).fillMaxWidth()) {
        val wide = maxWidth >= 840.dp && LocalDensity.current.fontScale <= 1.3f
        val intro: @Composable (Modifier) -> Unit = { modifier ->
            Column(
                modifier.background(colors.primarySoft, RoundedCornerShape(20.dp)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                PulseText(stringResource(R.string.pulse_garage_your_garage), style = PulseTextStyle.Overline, color = colors.onPrimarySoft)
                PulseText(
                    stringResource(R.string.pulse_garage_new_vehicle_title),
                    Modifier.semantics { heading() },
                    PulseTextStyle.Display,
                    colors.onPrimarySoft,
                )
                PulseText(stringResource(R.string.pulse_garage_vehicle_intro), color = colors.onPrimarySoft)
                if (wide) {
                    VehicleEditorArtwork(Modifier.fillMaxWidth().heightIn(min = 240.dp))
                    listOf(
                        R.string.pulse_garage_vehicle_benefit_care,
                        R.string.pulse_garage_vehicle_benefit_fuel,
                        R.string.pulse_garage_vehicle_benefit_details,
                    ).forEach { label ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painterResource(dev.abhishekan.pulse.core.ui.R.drawable.pulse_check),
                                null,
                                Modifier.size(24.dp),
                                colors.onPrimarySoft,
                            )
                            PulseText(stringResource(label), color = colors.onPrimarySoft)
                        }
                    }
                }
            }
        }
        val form: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PulseText(stringResource(R.string.pulse_garage_basic_details), Modifier.semantics { heading() }, PulseTextStyle.Title)
                PulseTextField(
                    draft["name"],
                    { change(draft.set("name", it)) },
                    stringResource(R.string.pulse_garage_vehicle_name),
                    Modifier.fillMaxWidth(),
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }),
                )
                VehicleOptions(
                    stringResource(R.string.pulse_garage_vehicle_type),
                    draft["vehicleType"],
                    garageVehicleTypes,
                    !busy,
                    { change(draft.set("vehicleType", it)) },
                )
                VehicleOptions(
                    stringResource(R.string.pulse_garage_vehicle_fuel),
                    draft["vehicleFuel"],
                    garageFuelTypes,
                    !busy,
                    { fuel ->
                        val updated = draft.set("vehicleFuel", fuel)
                        change(if (fuel == draft["vehicleSecondFuel"]) updated.set("vehicleSecondFuel", "") else updated)
                    },
                )
                PulseCheckbox(
                    draft["dualTank"] == "true",
                    { change(draft.set("dualTank", it.toString())) },
                    stringResource(R.string.pulse_garage_dual_tank),
                    enabled = !busy,
                )
                if (draft["dualTank"] == "true") {
                    GarageChoice(
                        stringResource(R.string.pulse_garage_second_fuel),
                        draft["vehicleSecondFuel"],
                        garageFuelTypes.filter { it != draft["vehicleFuel"] },
                        { change(draft.set("vehicleSecondFuel", it)) },
                        !busy,
                    )
                }
                HorizontalDivider(color = colors.border)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PulseText(
                        stringResource(R.string.pulse_garage_vehicle_pressure_title),
                        Modifier.semantics { heading() },
                        PulseTextStyle.Label,
                    )
                    PulseText(stringResource(R.string.pulse_garage_vehicle_pressure_hint), color = colors.textSecondary)
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val columns = if (maxWidth >= 360.dp && LocalDensity.current.fontScale <= 1.3f) 2 else 1
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(
                                "frontSolo" to R.string.pulse_garage_front_solo_psi,
                                "rearSolo" to R.string.pulse_garage_rear_solo_psi,
                                "frontPillion" to R.string.pulse_garage_front_pillion_psi,
                                "rearPillion" to R.string.pulse_garage_rear_pillion_psi,
                            ).chunked(columns).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    row.forEach { (key, label) ->
                                        PulseTextField(
                                            draft[key],
                                            { change(draft.set(key, it)) },
                                            stringResource(label),
                                            Modifier.weight(1f),
                                            enabled = !busy,
                                            keyboardOptions =
                                                KeyboardOptions(
                                                    keyboardType = KeyboardType.Decimal,
                                                    imeAction = ImeAction.Next,
                                                ),
                                            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                error?.let {
                    PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = colors.error)
                }
                PulseButton(
                    stringResource(R.string.pulse_garage_save_vehicle),
                    {
                        focus.clearFocus()
                        save()
                    },
                    Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    enabled = !busy && !blocked,
                    loading = busy,
                )
                PulseTextButton(stringResource(R.string.pulse_garage_cancel), {
                    focus.clearFocus()
                    cancel()
                }, Modifier.align(Alignment.CenterHorizontally), enabled = !busy)
            }
        }
        PulseCard(Modifier.fillMaxWidth()) {
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    intro(Modifier.weight(0.85f))
                    form(Modifier.weight(1.15f).padding(vertical = 8.dp))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    intro(Modifier.fillMaxWidth())
                    form(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun VehicleOptions(
    label: String,
    value: String,
    options: List<String>,
    enabled: Boolean,
    select: (String) -> Unit,
) {
    val colors = PulseTheme.colors
    val focus = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PulseText(label, style = PulseTextStyle.Label)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns =
                when {
                    LocalDensity.current.fontScale > 1.3f -> 1
                    options.size == 2 -> 2
                    maxWidth >= 480.dp -> 5
                    maxWidth >= 400.dp -> 3
                    else -> 2
                }
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { option ->
                            val selected = option == value
                            Surface(
                                modifier =
                                    Modifier
                                        .weight(
                                            1f,
                                        ).selectable(selected, enabled, Role.RadioButton, onClick = {
                                            focus.clearFocus()
                                            select(option)
                                        }),
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) colors.primarySoft else colors.surface,
                                border = BorderStroke(1.dp, if (selected) colors.action else colors.border),
                            ) {
                                Column(
                                    Modifier.heightIn(min = 64.dp).padding(12.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    PulseText(
                                        garageOption(option),
                                        style = PulseTextStyle.Label,
                                        color = if (enabled) colors.textPrimary else colors.textSecondary,
                                    )
                                }
                            }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
