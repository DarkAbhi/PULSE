package dev.abhishekan.pulse.feature.garage

import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiText

internal enum class EditorAction { Add, Edit, Save }
internal fun GarageForm.editorLabel(action: EditorAction): UiText = when (this) {
    GarageForm.Vehicle -> when (action) { EditorAction.Add -> uiText(R.string.pulse_garage_add_vehicle); EditorAction.Edit -> uiText(R.string.pulse_garage_edit_vehicle); EditorAction.Save -> uiText(R.string.pulse_garage_save_vehicle) }
    GarageForm.Fuel -> when (action) { EditorAction.Add -> uiText(R.string.pulse_garage_add_fuel); EditorAction.Edit -> uiText(R.string.pulse_garage_edit_fuel); EditorAction.Save -> uiText(R.string.pulse_garage_save_fuel) }
    GarageForm.Care -> when (action) { EditorAction.Add -> uiText(R.string.pulse_garage_add_care); EditorAction.Edit -> uiText(R.string.pulse_garage_edit_care); EditorAction.Save -> uiText(R.string.pulse_garage_save_care) }
    GarageForm.Pressure -> when (action) { EditorAction.Add -> uiText(R.string.pulse_garage_add_pressure); EditorAction.Edit -> uiText(R.string.pulse_garage_edit_pressure); EditorAction.Save -> uiText(R.string.pulse_garage_save_pressure) }
}
