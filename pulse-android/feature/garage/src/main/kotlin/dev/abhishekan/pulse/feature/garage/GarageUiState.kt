package dev.abhishekan.pulse.feature.garage

import dev.abhishekan.pulse.core.domain.model.*

enum class GarageTab(val label: String) { Overview("Overview"), Fuel("Fuel"), Care("Maintenance"), Air("Tires & air") }
data class GarageSection<T>(val value: T? = null, val loading: Boolean = true, val error: Failure? = null)
data class GarageUiState(val vehicles: GarageSection<List<Vehicle>> = GarageSection(), val history: GarageSection<VehicleHistory> = GarageSection(loading = false),
    val vehicle: Long? = null, val tab: GarageTab = GarageTab.Overview, val draft: GarageDraft? = null, val busy: Boolean = false,
    val progress: Float? = null, val uploadRecord: Long? = null, val error: String? = null, val message: String? = null, val reviewRequired: Boolean = false,
    val openUri: String? = null, val openType: String = "application/octet-stream") {
    val reviewReady get() = !vehicles.loading && vehicles.error == null && (vehicle == null || (!history.loading && history.error == null))
}
