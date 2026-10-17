package dev.abhishekan.pulse.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.usecase.GetHorizon
import dev.abhishekan.pulse.core.domain.usecase.GetNotifications
import dev.abhishekan.pulse.core.domain.usecase.GetProfile
import dev.abhishekan.pulse.core.domain.usecase.GetWorkoutToday
import dev.abhishekan.pulse.core.domain.usecase.ObserveFitnessChanges
import dev.abhishekan.pulse.core.domain.usecase.ObserveHorizonChanges
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getProfile: GetProfile,
    private val getWorkout: GetWorkoutToday,
    private val getHorizon: GetHorizon,
    private val getNotifications: GetNotifications,
    changes: ObserveFitnessChanges,
    horizonChanges: ObserveHorizonChanges,
) : ViewModel() {
    private val state = MutableStateFlow(HomeUiState())
    val uiState = state.asStateFlow()
    private val jobs = mutableMapOf<String, Job>()

    init {
        retryProfile()
        retryWorkout()
        retryHorizon()
        retryNotifications()
        viewModelScope.launch {
            horizonChanges().drop(1).collect {
                jobs["horizon"]?.cancel()
                retryHorizon()
            }
        }
        viewModelScope.launch {
            changes().drop(1).collect {
                jobs["workout"]?.cancel()
                retryWorkout()
            }
        }
    }

    private fun <T> load(
        key: String,
        fetch: suspend () -> Outcome<T>,
        empty: (T) -> Boolean,
        update: (HomeUiState, Section<T>) -> HomeUiState,
    ) {
        if (jobs[key]?.isActive == true) return
        state.update { update(it, Section.Loading) }
        jobs[key] =
            viewModelScope.launch {
                val section =
                    when (val result = fetch()) {
                        is Outcome.Error -> Section.Error(result.failure)
                        is Outcome.Success ->
                            if (empty(
                                    result.value,
                                )
                            ) {
                                Section.Empty
                            } else {
                                Section.Content(result.value)
                            }
                    }
                state.update { update(it, section) }
            }
    }

    fun retryProfile() =
        load("profile", {
            getProfile()
        }, { it.name == null }) { state, value -> state.copy(profile = value) }

    fun retryWorkout() =
        load("workout", {
            getWorkout()
        }, { !it.visited }) { state, value -> state.copy(workout = value) }

    fun retryHorizon() =
        load("horizon", {
            getHorizon()
        }, { false }) { state, value -> state.copy(horizon = value) }

    fun retryNotifications() =
        load("notifications", {
            getNotifications()
        }, { it.isEmpty() }) { state, value -> state.copy(notifications = value) }
}
