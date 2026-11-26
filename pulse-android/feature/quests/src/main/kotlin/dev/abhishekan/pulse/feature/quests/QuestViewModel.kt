package dev.abhishekan.pulse.feature.quests

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.QuestOverview
import dev.abhishekan.pulse.core.domain.model.QuestSettings
import dev.abhishekan.pulse.core.domain.repository.QuestRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate

internal data class QuestUiState(
    val overview: QuestOverview? = null,
    val loading: Boolean = true,
    val failure: Failure? = null,
    val saving: Boolean = false,
    val saveFailure: Failure? = null,
    val goalSaveVersion: Int = 0,
)

internal class QuestViewModel(
    private val repository: QuestRepository,
    private val saved: SavedStateHandle,
    private val clock: Clock,
) : ViewModel() {
    private val state = MutableStateFlow(QuestUiState())
    val uiState = state.asStateFlow()
    private var read: Job? = null
    private var rollover: Job? = null

    fun refresh() {
        if (state.value.saving || read?.isActive == true) return
        val week = saved.get<String>("questWeek")?.let(LocalDate::parse)
        read = viewModelScope.launch { load(week) }
    }

    fun selectWeek(week: LocalDate) {
        if (state.value.saving) return
        read?.cancel()
        saved["questWeek"] = week.toString()
        read = viewModelScope.launch { load(week) }
    }

    fun clearSaveFailure() {
        state.update { it.copy(saveFailure = null) }
    }

    fun save(settings: QuestSettings) {
        if (state.value.saving) return
        read?.cancel()
        state.update { it.copy(loading = false, saving = true, saveFailure = null) }
        viewModelScope.launch {
            when (val result = repository.saveSettings(settings)) {
                is Outcome.Success -> {
                    state.update { it.copy(goalSaveVersion = it.goalSaveVersion + 1) }
                    load(saved.get<String>("questWeek")?.let(LocalDate::parse))
                }
                is Outcome.Error -> state.update { it.copy(saveFailure = result.failure) }
            }
            state.update { it.copy(saving = false) }
        }
    }

    private suspend fun load(week: LocalDate?) {
        state.update { it.copy(loading = true, failure = null) }
        when (val result = repository.overview(week)) {
            is Outcome.Success -> {
                val data = result.value
                if (week != null && data.week.firstOrNull()?.date != week) saved.remove<String>("questWeek")
                state.update { it.copy(overview = data, loading = false) }
                scheduleRollover(data)
            }
            is Outcome.Error -> state.update { it.copy(loading = false, failure = result.failure) }
        }
    }

    private fun scheduleRollover(data: QuestOverview) {
        rollover?.cancel()
        val remaining = Duration.between(clock.instant(), data.nextResetAt).toMillis()
        if (remaining > 0) {
            rollover =
                viewModelScope.launch {
                    delay(remaining + 1_000)
                    refresh()
                }
        }
    }
}
