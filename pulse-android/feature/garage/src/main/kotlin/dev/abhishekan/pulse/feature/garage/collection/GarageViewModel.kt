package dev.abhishekan.pulse.feature.garage.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.Vehicle
import dev.abhishekan.pulse.core.domain.repository.GarageRepository
import dev.abhishekan.pulse.core.domain.usecase.ApplyGarageChange
import dev.abhishekan.pulse.core.domain.usecase.SearchVehicleCatalog
import dev.abhishekan.pulse.core.ui.UnsavedChanges
import dev.abhishekan.pulse.feature.garage.editor.GarageDraft
import dev.abhishekan.pulse.feature.garage.editor.GarageEditorViewModel
import dev.abhishekan.pulse.feature.garage.editor.GarageForm
import dev.abhishekan.pulse.feature.garage.shared.GarageSection
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
    private val state = MutableStateFlow<GarageSection<List<Vehicle>>>(GarageSection.Loading())
    val vehicles = state.asStateFlow()

    override val reviewReady get() = state.value.ready

    private var listJob: Job? = null
    private var hasResumed = false

    init {
        refresh()
    }

    fun onAction(action: GarageAction) {
        when (action) {
            GarageAction.Resume -> resume()
            GarageAction.AddVehicle -> edit(GarageDraft.new(GarageForm.Vehicle, 0))
            is GarageAction.Editor -> onEditorAction(action.action)
        }
    }

    private fun resume() {
        if (hasResumed) refresh() else hasResumed = true
    }

    protected override fun refresh() {
        listJob?.cancel()
        state.update { GarageSection.Loading(it.value) }
        listJob =
            viewModelScope.launch {
                val result = garage.vehicles()
                ensureActive()
                when (result) {
                    is Outcome.Success -> state.update { GarageSection.Ready(result.value) }
                    is Outcome.Error -> state.update { GarageSection.Failed(result.failure, it.value) }
                }
            }
    }
}
