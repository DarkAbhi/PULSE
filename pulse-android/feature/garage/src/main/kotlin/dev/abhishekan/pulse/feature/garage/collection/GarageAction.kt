package dev.abhishekan.pulse.feature.garage.collection

import dev.abhishekan.pulse.feature.garage.editor.GarageEditorAction

internal sealed interface GarageAction {
    data object Resume : GarageAction
    data object AddVehicle : GarageAction
    data class Editor(val action: GarageEditorAction) : GarageAction
}
