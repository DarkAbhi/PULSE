package dev.abhishekan.pulse.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.usecase.GetHorizon
import dev.abhishekan.pulse.core.domain.usecase.GetNotificationCount
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
    changes: ObserveFitnessChanges,
    horizonChanges: ObserveHorizonChanges,
    dashboardChanges: dev.abhishekan.pulse.core.domain.usecase.ObserveDashboardChanges,
    private val getNotificationCount: GetNotificationCount,
) : ViewModel() {
    private val state = MutableStateFlow(HomeUiState())
    val uiState = state.asStateFlow()
    private val jobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            dashboardChanges().drop(1).collect {
                jobs["profile"]?.cancel()
                retryProfile()
            }
        }
        retryProfile()
        retryWorkout()
        retryHorizon()
        refreshNotificationCount()
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

    fun refreshNotificationCount() {
        if (jobs["notifications"]?.isActive == true) return
        jobs["notifications"] =
            viewModelScope.launch {
                when (val result = getNotificationCount()) {
                    is Outcome.Success -> state.update { it.copy(notificationCount = result.value) }
                    is Outcome.Error -> Unit // Keep the last known count when a refresh fails.
                }
            }
    }

    private fun <T> load(
        key: String,
        fetch: suspend () -> Outcome<T>,
        empty: (T) -> Boolean,
        current: (HomeUiState) -> Section<T>,
        update: (HomeUiState, Section<T>) -> HomeUiState,
    ) {
        if (jobs[key]?.isActive == true) return
        val previous = current(state.value)
        val retained = previous is Section.Content || previous == Section.Empty
        state.update {
            val next = if (retained) it else update(it, Section.Loading)
            next.copy(refreshing = next.refreshing + key, refreshErrors = next.refreshErrors - key)
        }
        jobs[key] =
            viewModelScope.launch {
                val result = fetch()
                val section =
                    when (result) {
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
                state.update {
                    val refreshFailure = (result as? Outcome.Error)?.failure?.takeIf { retained }
                    val next = if (refreshFailure != null) it else update(it, section)
                    next.copy(
                        refreshing = next.refreshing - key,
                        refreshErrors =
                            if (refreshFailure != null) {
                                next.refreshErrors + (key to refreshFailure)
                            } else {
                                next.refreshErrors - key
                            },
                    )
                }
            }
    }

    fun retryProfile() =
        load("profile", {
            getProfile()
        }, { it.name == null && it.hasProfile }, { it.profile }) { state, value -> state.copy(profile = value) }

    fun retryWorkout() =
        load("workout", {
            getWorkout()
        }, { !it.visited }, { it.workout }) { state, value -> state.copy(workout = value) }

    fun retryHorizon() =
        load("horizon", {
            getHorizon()
        }, { false }, { it.horizon }) { state, value -> state.copy(horizon = value) }
}
