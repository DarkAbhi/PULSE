package dev.abhishekan.pulse.feature.garage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.FuelFill
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.garageVehicleKinds
import dev.abhishekan.pulse.core.domain.usecase.ApplyGarageChange
import dev.abhishekan.pulse.core.domain.usecase.SearchVehicleCatalog
import dev.abhishekan.pulse.core.ui.DraftSource
import dev.abhishekan.pulse.core.ui.UnsavedChanges
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DateTimeException

/** Shared draft, catalog search, and write handling for the two Garage destinations. */
internal abstract class GarageEditorViewModel(
    private val apply: ApplyGarageChange,
    protected val saved: SavedStateHandle,
    private val searchCatalog: SearchVehicleCatalog,
    unsavedChanges: UnsavedChanges,
) : ViewModel() {
    protected val editor =
        MutableStateFlow(
            GarageEditorState(
                draft = saved.get<ArrayList<String>>("draft")?.let(GarageDraft::restore),
                reviewRequired = saved["pending"] ?: false,
            ),
        )
    val editorState = editor.asStateFlow()

    private var makesJob: Job? = null
    private var modelsJob: Job? = null
    private var makesKey: List<String>? = null
    private var modelsKey: List<String>? = null
    private var matchJob: Job? = null
    private var matchKey: List<String>? = null

    protected abstract val reviewReady: Boolean

    abstract fun refresh()

    protected open fun existingFuel(): List<FuelFill> = emptyList()

    init {
        unsavedChanges.track(DraftSource.Garage, editor.map { it.draft?.dirty == true }, viewModelScope)
        refreshCatalog()
    }

    fun consumeMessage() {
        editor.update { it.copy(message = null) }
    }

    fun edit(draft: GarageDraft) {
        if (!editor.value.busy &&
            !editor.value.reviewRequired
        ) {
            storeDraft(draft)
        }
    }

    fun update(draft: GarageDraft) {
        if (!editor.value.busy) storeDraft(draft)
    }

    private fun storeDraft(draft: GarageDraft?) {
        saved["draft"] = draft?.saved()
        editor.update { it.copy(draft = draft, error = null, message = null) }
        refreshCatalog()
    }

    fun retryCatalog() {
        refreshCatalog(force = true)
    }

    private fun refreshCatalog(force: Boolean = false) {
        val draft = editor.value.draft
        val manualKey =
            if (draft?.kind == GarageForm.Vehicle &&
                draft["catalogMode"] == "manual" &&
                draft["vehicleKind"] in garageVehicleKinds &&
                listOf("makeName", "modelName").all { draft[it].isNotBlank() && draft[it].codePointCount(0, draft[it].length) <= 200 }
            ) {
                listOf(draft["vehicleKind"], draft["makeName"], draft["modelName"])
            } else {
                null
            }
        if (force || manualKey != matchKey) {
            matchJob?.cancel()
            matchKey = manualKey
            editor.update { it.copy(match = GarageSection(loading = manualKey != null)) }
            if (manualKey != null) {
                matchJob =
                    viewModelScope.launch {
                        delay(350)
                        val result = searchCatalog.match(manualKey[0], manualKey[1], manualKey[2])
                        if (matchKey == manualKey) editor.update { it.copy(match = result.section()) }
                    }
            }
        }
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
            editor.update { it.copy(makes = GarageSection(loading = makeKey != null)) }
            if (makeKey != null) {
                makesJob =
                    viewModelScope.launch {
                        delay(250)
                        val result = searchCatalog(makeKey[0], null, makeKey[1])
                        if (makesKey == makeKey) editor.update { it.copy(makes = result.section()) }
                    }
            }
        }
        if (force || modelKey != modelsKey) {
            modelsJob?.cancel()
            modelsKey = modelKey
            editor.update { it.copy(models = GarageSection(loading = modelKey != null)) }
            if (modelKey != null) {
                modelsJob =
                    viewModelScope.launch {
                        delay(250)
                        val result = searchCatalog(modelKey[0], modelKey[1], modelKey[2])
                        if (modelsKey == modelKey) editor.update { it.copy(models = result.section()) }
                    }
            }
        }
    }

    private fun <T> Outcome<T>.section(): GarageSection<T> =
        when (this) {
            is Outcome.Success -> GarageSection(value, loading = false)
            is Outcome.Error -> GarageSection(loading = false, error = failure)
        }

    fun discard() {
        if (!editor.value.busy) storeDraft(null)
    }

    fun save() {
        if (editor.value.busy || editor.value.reviewRequired) return
        val draft = editor.value.draft ?: return
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
            editor.update {
                it.copy(
                    error =
                        uiText(
                            R.string.pulse_garage_check_the_fields_a_name_title_is_required_quantities,
                        ),
                )
            }
            return
        }
        val existingFuel = existingFuel()
        val maxOdometer = apply.minimumOdometer(existingFuel)
        if (!apply.validOdometer(change, existingFuel)) {
            editor.update {
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
        write(change, closeDraft = true)
    }

    protected fun write(
        change: GarageChange,
        closeDraft: Boolean = false,
        onSuccess: () -> Unit = ::refresh,
    ) {
        if (editor.value.busy || editor.value.reviewRequired) return
        beginWrite()
        viewModelScope.launch { finishWrite(apply(change), closeDraft, onSuccess) }
    }

    protected fun beginWrite() {
        saved["pending"] = true
        editor.update { it.copy(busy = true, error = null, message = null) }
    }

    protected fun finishWrite(
        result: Outcome<Unit>,
        closeDraft: Boolean = false,
        onSuccess: () -> Unit = ::refresh,
    ) {
        when (result) {
            is Outcome.Success -> {
                saved["pending"] = false
                if (closeDraft) storeDraft(null)
                editor.update {
                    it.copy(
                        busy = false,
                        message = uiText(R.string.pulse_garage_saved_your_garage_is_up_to_date),
                    )
                }
            }
            is Outcome.Error ->
                editor.update {
                    it.copy(
                        busy = false,
                        reviewRequired = true,
                        message = uiText(R.string.pulse_garage_uncertain_change, result.failure.message),
                    )
                }
        }
        if (result is Outcome.Success) onSuccess() else refresh()
    }

    fun acknowledgeReview() {
        if (reviewReady && editor.value.reviewRequired) {
            saved["pending"] = false
            editor.update {
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
