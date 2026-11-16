package dev.abhishekan.pulse.feature.garage.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.domain.model.GarageRecord
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.usecase.ApplyGarageChange
import dev.abhishekan.pulse.core.domain.usecase.DownloadGarageReceipt
import dev.abhishekan.pulse.core.domain.usecase.GetVehicleHistory
import dev.abhishekan.pulse.core.domain.usecase.SearchVehicleCatalog
import dev.abhishekan.pulse.core.domain.usecase.UploadGarageReceipt
import dev.abhishekan.pulse.core.ui.UnsavedChanges
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.garage.R
import dev.abhishekan.pulse.feature.garage.editor.GarageDraft
import dev.abhishekan.pulse.feature.garage.editor.GarageEditorViewModel
import dev.abhishekan.pulse.feature.garage.editor.GarageForm
import dev.abhishekan.pulse.feature.garage.editor.fields
import dev.abhishekan.pulse.feature.garage.maintenance.reminders.ReminderPage
import dev.abhishekan.pulse.feature.garage.shared.GarageSection
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class VehicleDetailViewModel(
    private val getHistory: GetVehicleHistory,
    apply: ApplyGarageChange,
    private val uploadReceipt: UploadGarageReceipt,
    private val downloadReceipt: DownloadGarageReceipt,
    saved: SavedStateHandle,
    searchCatalog: SearchVehicleCatalog,
    unsavedChanges: UnsavedChanges,
) : GarageEditorViewModel(apply, saved, searchCatalog, unsavedChanges) {
    private val vehicleId = checkNotNull(saved.get<Long>("id"))
    private val state =
        MutableStateFlow(
            VehicleDetailUiState(
                vehicleId = vehicleId,
                tab = GarageTab.entries.firstOrNull { it.name == saved.get<String>("tab") } ?: GarageTab.Overview,
                reminderPage = ReminderPage.fromSavedKey(saved["reminderPage"]),
            ),
        )
    val uiState = state.asStateFlow()

    private val eventChannel = Channel<VehicleDetailEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    override val reviewReady get() = state.value.history.ready

    private var historyJob: Job? = null

    init {
        refresh()
    }

    fun onAction(action: VehicleDetailAction) {
        when (action) {
            is VehicleDetailAction.Editor -> onEditorAction(action.action)
            is VehicleDetailAction.Create -> edit(GarageDraft.new(action.form, vehicleId))
            is VehicleDetailAction.Edit -> edit(action.draft)
            is VehicleDetailAction.SelectTab -> tab(action.tab)
            is VehicleDetailAction.OpenReminders -> reminders(action.page)
            VehicleDetailAction.BackFromReminder -> backFromReminder()
            VehicleDetailAction.EditPressure -> pressure()
            is VehicleDetailAction.Mutate -> mutate(action.change)
            is VehicleDetailAction.UploadReceipt -> upload(action.record, action.source)
            is VehicleDetailAction.DownloadReceipt -> download(action.record, action.receipt, action.destination)
            is VehicleDetailAction.FileError -> editor.update { it.copy(message = action.message) }
        }
    }

    protected override fun refresh() {
        if (state.value.deleted) return
        historyJob?.cancel()
        state.update { it.copy(history = GarageSection.Loading(it.history.value)) }
        historyJob =
            viewModelScope.launch {
                val result = getHistory(vehicleId)
                ensureActive()
                when (result) {
                    is Outcome.Success -> state.update { it.copy(history = GarageSection.Ready(result.value)) }
                    is Outcome.Error ->
                        state.update {
                            val cached = it.history.value.takeUnless { result.failure == Failure.NotFound }
                            it.copy(history = GarageSection.Failed(result.failure, cached))
                        }
                }
            }
    }

    override fun existingFuel() =
        state.value.history.value
            ?.fuel
            .orEmpty()

    private fun mutate(change: GarageChange) {
        if (change is GarageChange.Delete && change.kind == GarageRecord.Vehicle) {
            write(change, onSuccess = {
                historyJob?.cancel()
                state.update { it.copy(deleted = true, history = GarageSection.Idle) }
            })
        } else {
            write(change)
        }
    }

    private fun tab(tab: GarageTab) {
        saved["tab"] = tab.name
        saved["reminderPage"] = null
        state.update { it.copy(tab = tab, reminderPage = null) }
    }

    private fun reminders(page: ReminderPage?) {
        if (editor.value.busy || editor.value.draft != null) return
        saved["reminderPage"] = page?.savedKey
        saved["tab"] = GarageTab.Care.name
        state.update { it.copy(tab = GarageTab.Care, reminderPage = page) }
    }

    private fun backFromReminder() {
        val page = state.value.reminderPage ?: return
        reminders(if (page == ReminderPage.List) null else ReminderPage.List)
    }

    private fun pressure() {
        state.value.history.value?.let {
            edit(
                GarageDraft(
                    GarageForm.Pressure,
                    vehicleId,
                    fields = it.pressure.fields(),
                ),
            )
        }
    }

    private fun upload(
        record: Long,
        source: String,
    ) {
        if (editor.value.busy || editor.value.reviewRequired) return
        beginWrite()
        state.update { it.copy(progress = 0f, uploadRecord = record) }
        viewModelScope.launch {
            val result =
                uploadReceipt(vehicleId, record, source) { value ->
                    state.update { it.copy(progress = value) }
                }
            state.update { it.copy(progress = null, uploadRecord = null) }
            finishWrite(result)
        }
    }

    private fun download(
        record: Long,
        receipt: GarageReceipt,
        destination: String?,
    ) {
        if (editor.value.busy) return
        editor.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val result = downloadReceipt(vehicleId, record, receipt, destination)
            if (result is Outcome.Success && destination == null) {
                editor.update { it.copy(busy = false) }
                eventChannel.send(VehicleDetailEvent.OpenReceipt(result.value, receipt.contentType))
                return@launch
            }
            val message =
                when (result) {
                    is Outcome.Success -> uiText(R.string.pulse_garage_receipt_downloaded_to_your_selected_document)
                    is Outcome.Error ->
                        uiText(
                            R.string.pulse_garage_receipt_unavailable_1_s_server_storage_may_be_unconfigured,
                            result.failure.message,
                        )
                }
            editor.update { it.copy(busy = false, message = message) }
        }
    }
}
