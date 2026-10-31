package dev.abhishekan.pulse.core.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DraftSource { Fitness, Horizon, Garage, MealPlan, Profile }

class UnsavedChanges {
    private val owners = MutableStateFlow(emptyMap<Any, DraftSource>())
    val sources: Flow<Set<DraftSource>> = owners.map { it.values.toSet() }.distinctUntilChanged()

    // Navigation retains draft ViewModels between tabs. Remove a report only when its owner is cleared.
    fun track(
        source: DraftSource,
        dirty: Flow<Boolean>,
        scope: CoroutineScope,
    ) {
        val owner = Any()
        scope.launch {
            try {
                dirty.distinctUntilChanged().collect { hasChanges ->
                    owners.update { if (hasChanges) it + (owner to source) else it - owner }
                }
            } finally {
                owners.update { it - owner }
            }
        }
    }
}
