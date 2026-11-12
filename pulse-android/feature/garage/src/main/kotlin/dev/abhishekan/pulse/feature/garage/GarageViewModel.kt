package dev.abhishekan.pulse.feature.garage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.Vehicle
import dev.abhishekan.pulse.core.domain.repository.GarageRepository
import dev.abhishekan.pulse.core.domain.usecase.ApplyGarageChange
import dev.abhishekan.pulse.core.domain.usecase.SearchVehicleCatalog
import dev.abhishekan.pulse.core.ui.UnsavedChanges
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class GarageViewModel(
    private val garage: GarageRepository,
    apply: ApplyGarageChange,
    saved: SavedStateHandle,
    searchCatalog: SearchVehicleCatalog,
    unsavedChanges: UnsavedChanges,
) : GarageEditorViewModel(apply, saved, searchCatalog, unsavedChanges) {
    private val state = MutableStateFlow(GarageSection<List<Vehicle>>())
    val vehicles = state.asStateFlow()

    override val reviewReady get() = state.value.ready

    private var listJob: Job? = null
    private var hasResumed = false

    init {
        refresh()
    }

    fun resume() {
        if (hasResumed) refresh() else hasResumed = true
    }

    override fun refresh() {
        listJob?.cancel()
        state.update { it.copy(loading = true, error = null) }
        listJob =
            viewModelScope.launch {
                val result = garage.vehicles()
                ensureActive()
                when (result) {
                    is Outcome.Success -> state.value = GarageSection(result.value, loading = false)
                    is Outcome.Error -> state.update { it.copy(loading = false, error = result.failure) }
                }
            }
    }

    fun addVehicle() {
        edit(GarageDraft.new(GarageForm.Vehicle, 0))
    }
}
