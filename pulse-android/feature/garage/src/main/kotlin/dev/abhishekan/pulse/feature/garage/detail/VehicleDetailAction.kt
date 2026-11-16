package dev.abhishekan.pulse.feature.garage.detail

import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.feature.garage.editor.GarageDraft
import dev.abhishekan.pulse.feature.garage.editor.GarageEditorAction
import dev.abhishekan.pulse.feature.garage.editor.GarageForm
import dev.abhishekan.pulse.feature.garage.maintenance.reminders.ReminderPage

internal sealed interface VehicleDetailAction {
    data class Editor(val action: GarageEditorAction) : VehicleDetailAction
    data class Create(val form: GarageForm) : VehicleDetailAction
    data class Edit(val draft: GarageDraft) : VehicleDetailAction
    data class SelectTab(val tab: GarageTab) : VehicleDetailAction
    data class OpenReminders(val page: ReminderPage) : VehicleDetailAction
    data object BackFromReminder : VehicleDetailAction
    data object EditPressure : VehicleDetailAction
    data class Mutate(val change: GarageChange) : VehicleDetailAction
    data class UploadReceipt(val record: Long, val source: String) : VehicleDetailAction
    data class DownloadReceipt(
        val record: Long,
        val receipt: GarageReceipt,
        val destination: String? = null,
    ) : VehicleDetailAction
    data class FileError(val message: UiText) : VehicleDetailAction
}
