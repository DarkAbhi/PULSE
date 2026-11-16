package dev.abhishekan.pulse.feature.garage.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogMatch
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogOption
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.feature.garage.R
import dev.abhishekan.pulse.feature.garage.fuel.FuelEditor
import dev.abhishekan.pulse.feature.garage.maintenance.MaintenanceEditor
import dev.abhishekan.pulse.feature.garage.shared.GarageSection
import dev.abhishekan.pulse.feature.garage.vehicle.VehicleEditor

@Composable
internal fun GarageEditor(
    draft: GarageDraft,
    busy: Boolean,
    error: UiText?,
    change: (GarageDraft) -> Unit,
    makes: GarageSection<List<VehicleCatalogOption>> = GarageSection.Idle,
    models: GarageSection<List<VehicleCatalogOption>> = GarageSection.Idle,
    retryCatalog: () -> Unit = {},
    match: GarageSection<VehicleCatalogMatch?> = GarageSection.Idle,
) {
    when (draft.kind) {
        GarageForm.Vehicle -> VehicleEditor(draft, busy, error, change, makes, models, retryCatalog, match)
        GarageForm.Fuel -> FuelEditor(draft, busy, error, change)
        GarageForm.Care -> MaintenanceEditor(draft, busy, error, change)
        GarageForm.Pressure -> PressureEditor(draft, busy, error, change)
    }
}

@Composable
private fun PressureEditor(
    draft: GarageDraft,
    busy: Boolean,
    error: UiText?,
    change: (GarageDraft) -> Unit,
) {
    val focus = LocalFocusManager.current
    PulseCard(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth()) {
        PulseText(stringResource(R.string.pulse_garage_target_tire_pressures_in_psi_leave_a_field_empty))
        for ((field, label) in pressureFields) {
            PulseTextField(
                draft[field],
                { change(draft.set(field, it)) },
                stringResource(label),
                Modifier.fillMaxWidth(),
                !busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }),
            )
        }
        error?.let {
            PulseText(
                it,
                Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = PulseTheme.colors.error,
            )
        }
    }
}

private val pressureFields =
    listOf(
        "frontSolo" to R.string.pulse_garage_front_solo_psi,
        "rearSolo" to R.string.pulse_garage_rear_solo_psi,
        "frontPillion" to R.string.pulse_garage_front_pillion_psi,
        "rearPillion" to R.string.pulse_garage_rear_pillion_psi,
    )
