package dev.abhishekan.pulse.feature.garage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogMatch
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogOption
import dev.abhishekan.pulse.core.domain.model.garageFuelTypes
import dev.abhishekan.pulse.core.domain.model.garageVehicleKinds
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseChoiceCard
import dev.abhishekan.pulse.core.ui.components.PulseSegmentedTabs
import dev.abhishekan.pulse.core.ui.components.PulseSwitch
import dev.abhishekan.pulse.core.ui.components.PulseText
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
    makes: GarageSection<List<VehicleCatalogOption>> = GarageSection(loading = false),
    models: GarageSection<List<VehicleCatalogOption>> = GarageSection(loading = false),
    retryCatalog: () -> Unit = {},
    match: GarageSection<VehicleCatalogMatch?> = GarageSection(loading = false),
) {
    val colors = PulseTheme.colors
    val spacing = PulseTheme.spacing
    val focus = LocalFocusManager.current
    val step = draft.vehicleStep
    val go: (Int) -> Unit = {
        focus.clearFocus()
        change(draft.copy(fields = draft.fields + ("vehicleStep" to it.toString())))
    }
    BackHandler(step > 0 && !busy) { go(step - 1) }
    val titles = listOf(R.string.pulse_garage_which_vehicle, R.string.pulse_garage_what_powers_it, R.string.pulse_garage_tyre_pressure)
    val stepLabels = listOf(R.string.pulse_garage_step_vehicle, R.string.pulse_garage_fuel, R.string.pulse_garage_step_optional)
    BoxWithConstraints(Modifier.widthIn(max = 1200.dp).fillMaxWidth()) {
        val wide = maxWidth >= 840.dp && LocalDensity.current.fontScale <= 1.3f
        val intro: @Composable (Modifier) -> Unit = { modifier ->
            Column(
                modifier.background(colors.primarySoft, PulseTheme.shapes.extraLarge).padding(spacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(spacing.cardPadding),
            ) {
                PulseText(
                    stringResource(if (draft.id == null) R.string.pulse_garage_new_vehicle_title else R.string.pulse_garage_edit_vehicle),
                    Modifier.semantics { heading() },
                    PulseTextStyle.Display,
                    colors.onPrimarySoft,
                )
                PulseText(stringResource(R.string.pulse_garage_vehicle_intro), color = colors.onPrimarySoft)
                if (wide) {
                    VehicleEditorArtwork(draft["vehicleKind"], Modifier.fillMaxWidth())
                    PulseCard(Modifier.fillMaxWidth()) {
                        PulseText(
                            draft["name"].ifBlank { stringResource(R.string.pulse_garage_your_next_vehicle) },
                            style = PulseTextStyle.Title,
                        )
                        VehicleDraftSummary(draft)
                    }
                }
            }
        }
        val form: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
                val stepText: @Composable () -> Unit = {
                    PulseText(
                        stringResource(R.string.pulse_garage_vehicle_step, step + 1, stringResource(stepLabels[step])),
                        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = PulseTextStyle.Body2,
                        color = colors.textSecondary,
                    )
                }
                if (wide) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                    ) {
                        PulseText(stringResource(titles[step]), Modifier.weight(1f).semantics { heading() }, PulseTextStyle.Heading1)
                        stepText()
                    }
                } else {
                    PulseText(stringResource(titles[step]), Modifier.semantics { heading() }, PulseTextStyle.Heading1)
                    stepText()
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                    repeat(3) { index ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(if (index <= step) colors.primary else colors.border, PulseTheme.shapes.pill),
                        )
                    }
                }
                when (step) {
                    0 -> {
                        PulseSegmentedTabs(
                            listOf(
                                stringResource(R.string.pulse_garage_choose_vehicle),
                                stringResource(R.string.pulse_garage_enter_manually),
                            ),
                            if (draft["catalogMode"] == "manual") 1 else 0,
                            { change(draft.set("catalogMode", if (it == 0) "catalog" else "manual")) },
                            Modifier.fillMaxWidth(),
                            enabled = !busy,
                        )
                        VehicleOptions(stringResource(R.string.pulse_garage_kind), draft["vehicleKind"], garageVehicleKinds, !busy) {
                            change(draft.set("vehicleKind", it))
                        }
                        VehicleCatalogFields(draft, makes, models, !busy, change, retryCatalog)
                        if (draft["catalogMode"] == "manual") {
                            when {
                                match.loading ->
                                    PulseText(
                                        stringResource(R.string.pulse_garage_checking_match),
                                        color = colors.textSecondary,
                                    )
                                match.error != null -> {
                                    PulseText(stringResource(R.string.pulse_garage_match_unavailable), color = colors.textSecondary)
                                    PulseButton(
                                        stringResource(R.string.pulse_garage_retry_match),
                                        retryCatalog,
                                        enabled = !busy,
                                        style = PulseButtonStyle.Text,
                                    )
                                }
                                match.value != null ->
                                    PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
                                        PulseText(
                                            stringResource(
                                                R.string.pulse_garage_catalog_match,
                                                match.value.makeName,
                                                match.value.modelName,
                                            ),
                                        )
                                        PulseButton(stringResource(R.string.pulse_garage_use_match), {
                                            change(draft.useMatch(match.value))
                                        }, enabled = !busy)
                                    }
                            }
                        }
                        PulseTextField(
                            draft["name"],
                            { change(draft.set("name", it)) },
                            stringResource(R.string.pulse_garage_vehicle_name),
                            Modifier.fillMaxWidth(),
                            enabled = !busy,
                            supportingText = { PulseText(stringResource(R.string.pulse_garage_name_hint)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }),
                        )
                    }
                    1 -> {
                        VehicleOptions(
                            stringResource(R.string.pulse_garage_vehicle_fuel),
                            draft["vehicleFuel"],
                            garageFuelTypes,
                            !busy,
                        ) { fuel ->
                            val updated = draft.set("vehicleFuel", fuel)
                            change(if (fuel == draft["vehicleSecondFuel"]) updated.set("vehicleSecondFuel", "") else updated)
                        }
                        PulseCard(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, colors.border)) {
                            PulseSwitch(
                                draft["dualTank"] == "true",
                                { change(draft.set("dualTank", it.toString())) },
                                stringResource(R.string.pulse_garage_dual_tank),
                                Modifier.fillMaxWidth(),
                                enabled = !busy,
                                supportingText = stringResource(R.string.pulse_garage_two_fuels_hint),
                                switchAtEnd = true,
                            )
                        }
                        if (draft["dualTank"] == "true") {
                            PulseCard(Modifier.fillMaxWidth().testTag("vehicle-secondary-fuel"), highlighted = true) {
                                VehicleOptions(
                                    stringResource(R.string.pulse_garage_second_fuel),
                                    draft["vehicleSecondFuel"],
                                    garageFuelTypes.filter { it != draft["vehicleFuel"] },
                                    !busy,
                                ) { change(draft.set("vehicleSecondFuel", it)) }
                            }
                        }
                    }
                    2 -> {
                        PulseText(stringResource(R.string.pulse_garage_vehicle_pressure_hint), color = colors.textSecondary)
                        VehiclePressureFields(draft, !busy, change)
                        PulseCard(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, colors.border)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                                    PulseText(stringResource(R.string.pulse_garage_review), style = PulseTextStyle.Label)
                                    PulseText(draft["name"], style = PulseTextStyle.Title)
                                    VehicleDraftSummary(draft)
                                }
                                PulseButton(
                                    stringResource(R.string.pulse_garage_edit_vehicle),
                                    { go(0) },
                                    enabled = !busy,
                                    style = PulseButtonStyle.Text,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(spacing.small))
                error?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = colors.error) }
                if (step == 2) {
                    PulseButton(
                        stringResource(R.string.pulse_garage_save_vehicle),
                        {
                            focus.clearFocus()
                            save()
                        },
                        Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        enabled =
                            !busy && !blocked && draft.vehicleIdentityValid && draft.vehicleFuelValid,
                        loading = busy,
                    )
                } else {
                    PulseButton(
                        stringResource(if (step == 0) R.string.pulse_garage_continue_fuel else R.string.pulse_garage_continue_tyres),
                        { go(step + 1) },
                        Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        enabled = !busy && (if (step == 0) draft.vehicleIdentityValid else draft.vehicleFuelValid),
                    )
                }
                if (step == 0) {
                    VehicleDataCredits(Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                if (step > 0) {
                    PulseButton(stringResource(R.string.pulse_garage_step_back), {
                        go(step - 1)
                    }, Modifier.fillMaxWidth(), enabled = !busy, variant = PulseButtonVariant.Secondary)
                }
            }
        }
        PulseCard(Modifier.fillMaxWidth()) {
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.large)) {
                    intro(Modifier.weight(0.7f))
                    form(Modifier.weight(1.3f).padding(vertical = spacing.small))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.cardPadding)) {
                    intro(Modifier.fillMaxWidth())
                    form(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun VehicleDraftSummary(draft: GarageDraft) {
    val values =
        listOf(draft["vehicleKind"], draft["vehicleFuel"], draft["vehicleSecondFuel"].takeIf { draft["dualTank"] == "true" }.orEmpty())
    PulseText(values.filter(String::isNotBlank).map { garageOption(it) }.joinToString(" · "), color = PulseTheme.colors.textSecondary)
}

@Composable
internal fun VehicleOptions(
    label: String,
    value: String,
    options: List<String>,
    enabled: Boolean,
    select: (String) -> Unit,
) {
    val spacing = PulseTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        PulseText(label, style = PulseTextStyle.Label)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns =
                when {
                    LocalDensity.current.fontScale > 1.3f -> 1
                    maxWidth >= 600.dp && LocalDensity.current.fontScale <= 1.1f -> options.size
                    maxWidth >= 560.dp -> 3
                    else -> 2
                }
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                options.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                        row.forEach { option ->
                            PulseChoiceCard(garageOption(option), option == value, { select(option) }, Modifier.weight(1f), enabled) {
                                Icon(painterResource(vehicleOptionIcon(option)), null, Modifier.size(28.dp), PulseTheme.colors.textPrimary)
                            }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

private fun vehicleOptionIcon(option: String): Int =
    when (option) {
        "car" -> R.drawable.garage_option_car
        "motorcycle" -> R.drawable.garage_option_motorcycle
        "moped" -> R.drawable.garage_option_moped
        "van" -> R.drawable.garage_option_van
        "truck" -> R.drawable.garage_option_truck
        "bus" -> R.drawable.garage_option_bus
        "electric" -> R.drawable.garage_option_electric
        else -> R.drawable.garage_option_fuel
    }
