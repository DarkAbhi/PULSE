package dev.abhishekan.pulse.feature.garage.detail

import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.feature.garage.maintenance.reminders.ReminderPage
import dev.abhishekan.pulse.feature.garage.shared.GarageSection

internal data class VehicleDetailUiState(
    val vehicleId: Long,
    val history: GarageSection<VehicleHistory> = GarageSection.Loading(),
    val tab: GarageTab = GarageTab.Overview,
    val reminderPage: ReminderPage? = null,
    val progress: Float? = null,
    val uploadRecord: Long? = null,
    val deleted: Boolean = false,
)
