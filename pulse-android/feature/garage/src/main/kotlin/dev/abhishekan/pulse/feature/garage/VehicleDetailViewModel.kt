package dev.abhishekan.pulse.feature.garage

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
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.UnsavedChanges
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                tab = GarageTab.valueOf(saved["tab"] ?: GarageTab.Overview.name),
                reminderPage = saved["reminderPage"],
            ),
        )
    val uiState = state.asStateFlow()

    override val reviewReady get() = state.value.history.ready

    private var historyJob: Job? = null

    init {
        refresh()
    }

    override fun refresh() {
        if (state.value.deleted) return
        historyJob?.cancel()
        state.update { it.copy(history = it.history.copy(loading = true, error = null)) }
        historyJob =
            viewModelScope.launch {
                val result = getHistory(vehicleId)
                ensureActive()
                when (result) {
                    is Outcome.Success -> state.update { it.copy(history = GarageSection(result.value, loading = false)) }
                    is Outcome.Error ->
                        state.update {
                            val history = if (result.failure == Failure.NotFound) GarageSection() else it.history
                            it.copy(history = history.copy(loading = false, error = result.failure))
                        }
                }
            }
    }

    override fun existingFuel() =
        state.value.history.value
            ?.fuel
            .orEmpty()

    fun create(kind: GarageForm) {
        edit(GarageDraft.new(kind, vehicleId))
    }

    fun mutate(change: GarageChange) {
        if (change is GarageChange.Delete && change.kind == GarageRecord.Vehicle) {
            write(change, onSuccess = {
                historyJob?.cancel()
                state.update { it.copy(deleted = true, history = GarageSection(loading = false)) }
            })
        } else {
            write(change)
        }
    }

    fun tab(tab: GarageTab) {
        saved["tab"] = tab.name
        saved["reminderPage"] = null
        state.update { it.copy(tab = tab, reminderPage = null) }
    }

    fun reminders(page: String?) {
        if (editor.value.busy || editor.value.draft != null) return
        require(page == null || page in listOf("list", "engine_oil", "tire_pressure", "general_service"))
        saved["reminderPage"] = page
        saved["tab"] = GarageTab.Care.name
        state.update { it.copy(tab = GarageTab.Care, reminderPage = page) }
    }

    fun backFromReminder() = reminders(if (state.value.reminderPage == "list") null else "list")

    fun pressure() {
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

    fun upload(
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

    fun download(
        record: Long,
        receipt: GarageReceipt,
        destination: String? = null,
    ) {
        if (editor.value.busy) return
        editor.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val message =
                when (val result = downloadReceipt(vehicleId, record, receipt, destination)) {
                    is Outcome.Success -> {
                        if (destination == null) {
                            state.update { it.copy(openUri = result.value, openType = receipt.contentType) }
                            null
                        } else {
                            uiText(R.string.pulse_garage_receipt_downloaded_to_your_selected_document)
                        }
                    }
                    is Outcome.Error ->
                        uiText(
                            R.string.pulse_garage_receipt_unavailable_1_s_server_storage_may_be_unconfigured,
                            result.failure.message,
                        )
                }
            editor.update { it.copy(busy = false, message = message) }
        }
    }

    fun opened() {
        state.update { it.copy(openUri = null) }
    }

    fun fileMessage(message: UiText) {
        editor.update { it.copy(message = message) }
    }
}
