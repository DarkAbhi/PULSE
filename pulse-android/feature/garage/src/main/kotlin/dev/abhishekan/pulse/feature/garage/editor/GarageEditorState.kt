package dev.abhishekan.pulse.feature.garage.editor

import dev.abhishekan.pulse.core.domain.model.VehicleCatalogMatch
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogOption
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.feature.garage.shared.GarageSection

internal data class GarageEditorState(
    val draft: GarageDraft? = null,
    val makes: GarageSection<List<VehicleCatalogOption>> = GarageSection.Idle,
    val models: GarageSection<List<VehicleCatalogOption>> = GarageSection.Idle,
    val match: GarageSection<VehicleCatalogMatch?> = GarageSection.Idle,
    val busy: Boolean = false,
    val error: UiText? = null,
    val message: UiText? = null,
    val reviewRequired: Boolean = false,
)
