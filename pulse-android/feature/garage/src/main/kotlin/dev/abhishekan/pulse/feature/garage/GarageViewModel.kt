package dev.abhishekan.pulse.feature.garage
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogOption
import dev.abhishekan.pulse.core.domain.model.garageVehicleKinds
import dev.abhishekan.pulse.core.domain.usecase.ApplyGarageChange
import dev.abhishekan.pulse.core.domain.usecase.DownloadGarageReceipt
import dev.abhishekan.pulse.core.domain.usecase.GetVehicleHistory
import dev.abhishekan.pulse.core.domain.usecase.GetVehicles
import dev.abhishekan.pulse.core.domain.usecase.SearchVehicleCatalog
import dev.abhishekan.pulse.core.domain.usecase.UploadGarageReceipt
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val searchCatalog: SearchVehicleCatalog,
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

    fun consumeMessage() {
        state.update { it.copy(message = null) }
    }

    private var listJob: Job? = null
    private var historyJob: Job? = null
    private var makesJob: Job? = null
    private var modelsJob: Job? = null
    private var makesKey: List<String>? = null
    private var modelsKey: List<String>? = null

    init {
        refresh()
        refreshCatalog()
    }

    fun refresh() {
        refreshVehicles()
        refreshHistory()
    }

    fun refreshVehicles() {
        listJob?.cancel()
        state.update { it.copy(vehicles = it.vehicles.copy(loading = true, error = null)) }
        listJob =
            viewModelScope.launch {
                when (val result = getVehicles()) {
                    is Outcome.Error ->
                        state.update {
                            it.copy(vehicles = it.vehicles.copy(loading = false, error = result.failure))
                        }
                    is Outcome.Success -> {
                        if (state.value.vehicle != null &&
                            result.value.none { it.id == state.value.vehicle }
                        ) {
                            clearSelection()
                            state.update { it.copy(message = Failure.NotFound.message) }
                        }
                        state.update { it.copy(vehicles = GarageSection(result.value, false)) }
                    }
                }
            }
    }

    fun refreshHistory() {
        historyJob?.cancel()
        val id = state.value.vehicle ?: return
        state.update { it.copy(history = it.history.copy(loading = true, error = null)) }
        historyJob =
            viewModelScope.launch {
                when (val result = getHistory(id)) {
                    is Outcome.Error -> {
                        if (result.failure == Failure.NotFound) {
                            clearSelection()
                            state.update { it.copy(message = Failure.NotFound.message) }
                            refreshVehicles()
                        } else {
                            state.update {
                                it.copy(
                                    history = it.history.copy(loading = false, error = result.failure),
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
            state.update {
                it.copy(
                    vehicle = id,
                    message = null,
                    history = if (it.vehicle == id) it.history else GarageSection(),
                )
            }
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
        refreshCatalog()
    }

    fun retryCatalog() {
        refreshCatalog(force = true)
    }

    private fun refreshCatalog(force: Boolean = false) {
        val draft = state.value.draft
        val enabled = draft?.kind == GarageForm.Vehicle && draft["catalogMode"] != "manual" && draft["vehicleKind"] in garageVehicleKinds
        val makeKey = if (enabled) listOf(draft["vehicleKind"], draft["makeQuery"]) else null
        val modelKey =
            if (enabled &&
                !draft["makeSlug"].isBlank()
            ) {
                listOf(draft["vehicleKind"], draft["makeSlug"], draft["modelQuery"])
            } else {
                null
            }
        if (force || makeKey != makesKey) {
            makesJob?.cancel()
            makesKey = makeKey
            state.update { it.copy(makes = GarageSection(loading = makeKey != null)) }
            if (makeKey != null) {
                makesJob =
                    viewModelScope.launch {
                        delay(250)
                        val result = searchCatalog(makeKey[0], null, makeKey[1])
                        if (makesKey == makeKey) state.update { it.copy(makes = result.section()) }
                    }
            }
        }
        if (force || modelKey != modelsKey) {
            modelsJob?.cancel()
            modelsKey = modelKey
            state.update { it.copy(models = GarageSection(loading = modelKey != null)) }
            if (modelKey != null) {
                modelsJob =
                    viewModelScope.launch {
                        delay(250)
                        val result = searchCatalog(modelKey[0], modelKey[1], modelKey[2])
                        if (modelsKey == modelKey) state.update { it.copy(models = result.section()) }
                    }
            }
        }
    }

    private fun Outcome<List<VehicleCatalogOption>>.section(): GarageSection<List<VehicleCatalogOption>> =
        when (this) {
            is Outcome.Success -> GarageSection(value, loading = false)
            is Outcome.Error -> GarageSection(loading = false, error = failure)
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
