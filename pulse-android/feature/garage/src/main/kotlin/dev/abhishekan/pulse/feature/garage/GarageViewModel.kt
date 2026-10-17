package dev.abhishekan.pulse.feature.garage
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.usecase.ApplyGarageChange
import dev.abhishekan.pulse.core.domain.usecase.DownloadGarageReceipt
import dev.abhishekan.pulse.core.domain.usecase.GetVehicleHistory
import dev.abhishekan.pulse.core.domain.usecase.GetVehicles
import dev.abhishekan.pulse.core.domain.usecase.UploadGarageReceipt
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.garage.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DateTimeException

class GarageViewModel(
    private val getVehicles: GetVehicles,
    private val getHistory: GetVehicleHistory,
    private val apply: ApplyGarageChange,
    private val uploadReceipt: UploadGarageReceipt,
    private val downloadReceipt: DownloadGarageReceipt,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val state =
        MutableStateFlow(
            GarageUiState(
                vehicle = saved["vehicle"],
                tab =
                    GarageTab.valueOf(
                        saved["tab"] ?: "Overview",
                    ),
                draft = saved.get<ArrayList<String>>("draft")?.let(GarageDraft::restore),
                reviewRequired =
                    saved["pending"] ?: false,
            ),
        )
    val uiState = state.asStateFlow()
    private var listJob: Job? = null
    private var historyJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        refreshVehicles()
        refreshHistory()
    }

    fun refreshVehicles() {
        listJob?.cancel()
        state.update { it.copy(vehicles = GarageSection()) }
        listJob =
            viewModelScope.launch {
                when (val result = getVehicles()) {
                    is Outcome.Error ->
                        state.update {
                            it.copy(vehicles = GarageSection(loading = false, error = result.failure))
                        }
                    is Outcome.Success -> {
                        if (state.value.vehicle != null &&
                            result.value.none { it.id == state.value.vehicle }
                        ) {
                            clearSelection()
                        }
                        state.update { it.copy(vehicles = GarageSection(result.value, false)) }
                    }
                }
            }
    }

    fun refreshHistory() {
        historyJob?.cancel()
        val id = state.value.vehicle ?: return
        state.update { it.copy(history = GarageSection()) }
        historyJob =
            viewModelScope.launch {
                when (val result = getHistory(id)) {
                    is Outcome.Error -> {
                        if (result.failure == Failure.NotFound) {
                            clearSelection()
                            refreshVehicles()
                        } else {
                            state.update {
                                it.copy(
                                    history = GarageSection(loading = false, error = result.failure),
                                )
                            }
                        }
                    }
                    is Outcome.Success ->
                        state.update {
                            it.copy(history = GarageSection(result.value, false))
                        }
                }
            }
    }

    private fun clearSelection() {
        saved["vehicle"] = null
        historyJob?.cancel()
        state.update { it.copy(vehicle = null, history = GarageSection(loading = false)) }
    }

    fun select(id: Long?) {
        if (state.value.busy || state.value.draft != null) return
        if (id ==
            null
        ) {
            clearSelection()
        } else {
            saved["vehicle"] = id
            state.update { it.copy(vehicle = id, message = null) }
            refreshHistory()
        }
    }

    fun tab(tab: GarageTab) {
        saved["tab"] = tab.name
        state.update { it.copy(tab = tab) }
    }

    fun edit(draft: GarageDraft) {
        if (!state.value.busy &&
            !state.value.reviewRequired
        ) {
            storeDraft(draft)
        }
    }

    fun update(draft: GarageDraft) {
        if (!state.value.busy) storeDraft(draft)
    }

    private fun storeDraft(draft: GarageDraft?) {
        saved["draft"] = draft?.saved()
        state.update { it.copy(draft = draft, error = null, message = null) }
    }

    fun discard() {
        if (!state.value.busy) storeDraft(null)
    }

    fun create(kind: GarageForm) {
        edit(GarageDraft.new(kind, state.value.vehicle ?: 0))
    }

    fun pressure() {
        state.value.history.value?.let {
            edit(
                GarageDraft(
                    GarageForm.Pressure,
                    state.value.vehicle ?: 0,
                    fields = it.pressure.fields(),
                ),
            )
        }
    }

    fun save() {
        if (state.value.busy || state.value.reviewRequired) return
        val draft = state.value.draft ?: return
        val change =
            try {
                draft.change()
            } catch (
                _: IllegalArgumentException,
            ) {
                null
            } catch (_: DateTimeException) {
                null
            }
        if (change == null || !apply.valid(change)) {
            state.update {
                it.copy(
                    error =
                        uiText(
                            R.string.pulse_garage_check_the_fields_a_name_title_is_required_quantities,
                        ),
                )
            }
            return
        }
        val existingFuel =
            state.value.history.value
                ?.fuel
                .orEmpty()
        val maxOdometer = apply.minimumOdometer(existingFuel)
        if (!apply.validOdometer(change, existingFuel)) {
            state.update {
                it.copy(
                    error =
                        uiText(
                            R.string.pulse_garage_odometer_must_be_at_least_1_s_km_the,
                            maxOdometer?.toPlainString(),
                        ),
                )
            }
            return
        }
        mutate(change, true)
    }

    fun mutate(
        change: GarageChange,
        closeDraft: Boolean = false,
    ) {
        if (state.value.busy || state.value.reviewRequired) return
        beginWrite()
        viewModelScope.launch { finishWrite(apply(change), closeDraft) }
    }

    private fun beginWrite() {
        saved["pending"] = true
        state.update { it.copy(busy = true, error = null, message = null) }
    }

    private fun finishWrite(
        result: Outcome<Unit>,
        closeDraft: Boolean = false,
    ) {
        when (result) {
            is Outcome.Success -> {
                saved["pending"] = false
                if (closeDraft) storeDraft(null)
                state.update {
                    it.copy(
                        busy = false,
                        progress = null,
                        uploadRecord = null,
                        message = uiText(R.string.pulse_garage_saved_your_garage_is_up_to_date),
                    )
                }
            }
            is Outcome.Error ->
                state.update {
                    it.copy(
                        busy = false,
                        progress = null,
                        uploadRecord = null,
                        reviewRequired = true,
                        message = uiText(R.string.pulse_garage_uncertain_change, result.failure.message),
                    )
                }
        }
        refresh()
    }

    fun upload(
        vehicle: Long,
        record: Long,
        source: String,
    ) {
        if (state.value.busy || state.value.reviewRequired) return
        beginWrite()
        state.update { it.copy(progress = 0f, uploadRecord = record) }
        viewModelScope.launch {
            finishWrite(
                uploadReceipt(vehicle, record, source) { value ->
                    state.update { it.copy(progress = value) }
                },
            )
        }
    }

    fun download(
        record: Long,
        receipt: GarageReceipt,
        destination: String? = null,
        vehicle: Long? = state.value.vehicle,
    ) {
        if (state.value.busy) return
        val selectedVehicle = vehicle ?: return
        state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            when (val result = downloadReceipt(selectedVehicle, record, receipt, destination)) {
                is Outcome.Success ->
                    state.update {
                        it.copy(
                            busy = false,
                            openUri =
                                result.value.takeIf {
                                    destination ==
                                        null
                                },
                            openType = receipt.contentType,
                            message =
                                if (destination ==
                                    null
                                ) {
                                    null
                                } else {
                                    uiText(
                                        R.string.pulse_garage_receipt_downloaded_to_your_selected_document,
                                    )
                                },
                        )
                    }
                is Outcome.Error ->
                    state.update {
                        it.copy(
                            busy = false,
                            message =
                                uiText(
                                    R.string.pulse_garage_receipt_unavailable_1_s_server_storage_may_be_unconfigured,
                                    result.failure.message,
                                ),
                        )
                    }
            }
        }
    }

    fun opened() {
        state.update { it.copy(openUri = null) }
    }

    fun fileMessage(message: UiText) {
        state.update { it.copy(message = message) }
    }

    fun acknowledgeReview() {
        if (state.value.reviewReady &&
            state.value.reviewRequired
        ) {
            saved["pending"] = false
            state.update {
                it.copy(
                    reviewRequired = false,
                    message =
                        uiText(
                            R.string.pulse_garage_records_reviewed_discard_the_draft_if_it_was_already,
                        ),
                )
            }
        }
    }
}
