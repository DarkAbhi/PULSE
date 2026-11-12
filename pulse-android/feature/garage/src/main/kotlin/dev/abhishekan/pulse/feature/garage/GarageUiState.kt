package dev.abhishekan.pulse.feature.garage
import androidx.annotation.StringRes
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogMatch
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogOption
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.core.ui.UiText

internal enum class GarageTab(
    @param:StringRes val label: Int,
) {
    Overview(
        R.string.pulse_garage_overview,
    ),
    Fuel(
        R.string.pulse_garage_fuel,
    ),
    Care(R.string.pulse_garage_maintenance),
    Air(R.string.pulse_garage_tires_air),
}

internal data class GarageSection<T>(
    val value: T? = null,
    val loading: Boolean = true,
    val error: Failure? = null,
) {
    val ready get() = value != null && !loading && error == null
}

internal data class GarageEditorState(
    val draft: GarageDraft? = null,
    val makes: GarageSection<List<VehicleCatalogOption>> = GarageSection(loading = false),
    val models: GarageSection<List<VehicleCatalogOption>> = GarageSection(loading = false),
    val match: GarageSection<VehicleCatalogMatch?> = GarageSection(loading = false),
    val busy: Boolean = false,
    val error: UiText? = null,
    val message: UiText? = null,
    val reviewRequired: Boolean = false,
)

internal data class VehicleDetailUiState(
    val vehicleId: Long,
    val history: GarageSection<VehicleHistory> = GarageSection(),
    val tab: GarageTab = GarageTab.Overview,
    val reminderPage: String? = null,
    val progress: Float? = null,
    val uploadRecord: Long? = null,
    val openUri: String? = null,
    val openType: String = "application/octet-stream",
    val deleted: Boolean = false,
)
