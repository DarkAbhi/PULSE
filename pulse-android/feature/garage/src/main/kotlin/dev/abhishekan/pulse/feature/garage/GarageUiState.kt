package dev.abhishekan.pulse.feature.garage
import androidx.annotation.StringRes
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Vehicle
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.core.ui.UiText

enum class GarageTab(
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

data class GarageSection<T>(
    val value: T? = null,
    val loading: Boolean = true,
    val error: Failure? = null,
)

data class GarageUiState(
    val vehicles: GarageSection<List<Vehicle>> = GarageSection(),
    val history: GarageSection<VehicleHistory> = GarageSection(loading = false),
    val vehicle: Long? = null,
    val tab: GarageTab = GarageTab.Overview,
    val draft: GarageDraft? = null,
    val busy: Boolean = false,
    val progress: Float? = null,
    val uploadRecord: Long? = null,
    val error: UiText? = null,
    val message: UiText? = null,
    val reviewRequired: Boolean = false,
    val openUri: String? = null,
    val openType: String = "application/octet-stream",
) {
    val reviewReady get() =
        !vehicles.loading &&
            vehicles.error == null &&
            (vehicle == null || (!history.loading && history.error == null))
}
