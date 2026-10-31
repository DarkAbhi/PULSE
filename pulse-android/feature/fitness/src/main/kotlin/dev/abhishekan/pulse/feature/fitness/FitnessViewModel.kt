package dev.abhishekan.pulse.feature.fitness
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.FitnessDates
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.usecase.GetFitnessOverview
import dev.abhishekan.pulse.core.domain.usecase.ObserveFitnessChanges
import dev.abhishekan.pulse.core.domain.usecase.RecordWorkout
import dev.abhishekan.pulse.core.ui.uiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

class FitnessViewModel(
    private val getOverview: GetFitnessOverview,
    private val record: RecordWorkout,
    changes: ObserveFitnessChanges,
    private val saved: SavedStateHandle,
    private val clock: Clock,
) : ViewModel() {
    private val selected =
        saved.get<String>("selected")?.let(LocalDate::parse) ?: FitnessDates.today(clock)
    private val state =
        MutableStateFlow(
            FitnessUiState(
                selected,
                YearMonth.from(selected),
                needsReview =
                    saved["pending"] ?: false,
            ),
        )
    val uiState = state.asStateFlow()

    fun consumeMessage() {
        state.update { it.copy(message = null) }
    }

    private var selectionVersion = 0L
    private var firstLoad = saved.get<String>("selected") == null
    private var loadJob: Job? = null
    private var hasResumed = false

    init {
        refresh()
        viewModelScope.launch {
            changes().drop(1).collect { if (!state.value.busy) refresh(force = true) }
        }
    }

    fun select(date: LocalDate) {
        if (state.value.busy) return
        selectionVersion++
        saved["selected"] = date.toString()
        state.update {
            it.copy(
                selected = date,
                month = YearMonth.from(date),
                createdId = null,
                message = if (it.needsReview) it.message else null,
            )
        }
    }

    fun shiftMonth(offset: Long) {
        val next = state.value.month.plusMonths(offset)
        if (next.year in 0..9999) select(next.atDay(1))
    }

    fun selectToday() {
        val current = state.value
        if (current.busy) return
        val today = FitnessDates.today(clock)
        if (current.selected == today && current.overview?.today == today) return
        loadJob?.cancel()
        val version = selectionVersion
        loadJob =
            viewModelScope.launch {
                if (load() &&
                    version == selectionVersion
                ) {
                    select(checkNotNull(state.value.overview).today)
                }
            }
    }

    fun resume() {
        if (hasResumed) refresh() else hasResumed = true
    }

    fun refresh(force: Boolean = false) {
        if (state.value.busy || (!force && loadJob?.isActive == true)) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch { load() }
    }

    private suspend fun load(): Boolean {
        state.update { it.copy(loading = true, error = null, reviewReady = false) }
        return when (val result = getOverview()) {
            is Outcome.Success -> {
                state.update {
                    it.copy(overview = result.value, loading = false, reviewReady = it.needsReview)
                }
                if (firstLoad) {
                    firstLoad = false
                    selectAfterRecord(result.value.today)
                }
                true
            }
            is Outcome.Error -> {
                state.update { it.copy(loading = false, error = result.failure) }
                false
            }
        }
    }

    fun acknowledgeReview() {
        if (!state.value.reviewReady || state.value.busy) return
        saved["pending"] = false
        state.update { it.copy(needsReview = false, reviewReady = false, message = null) }
    }

    fun recordSelected() {
        val input = state.value
        if (input.busy ||
            input.loading ||
            input.needsReview ||
            input.overview == null ||
            input.error != null
        ) {
            return
        }
        loadJob?.cancel()
        val todayAction =
            input.selected == input.overview.today && input.selected == FitnessDates.today(clock)
        saved["pending"] = true
        state.update { it.copy(busy = true, message = null, createdId = null) }
        viewModelScope.launch {
            when (val result = record(if (todayAction) null else input.selected)) {
                is Outcome.Success -> {
                    saved["pending"] = false
                    state.update {
                        it.copy(
                            createdId = result.value,
                            message =
                                uiText(
                                    R.string.pulse_fitness_workout_recorded_a_little_effort_a_little_momentum,
                                ),
                        )
                    }
                    if (load() &&
                        todayAction
                    ) {
                        selectAfterRecord(checkNotNull(state.value.overview).today)
                    }
                }
                is Outcome.Error -> {
                    val uncertain =
                        result.failure !in
                            setOf(Failure.InvalidInput, Failure.Unauthorized, Failure.NotFound)
                    saved["pending"] = uncertain
                    state.update {
                        it.copy(
                            needsReview = uncertain,
                            message =
                                if (uncertain) {
                                    uiText(
                                        R.string.pulse_fitness_the_response_was_interrupted_refresh_and_review_your_workouts,
                                    )
                                } else {
                                    fitnessFailureMessage(result.failure)
                                },
                        )
                    }
                    load()
                }
            }
            state.update { it.copy(busy = false) }
        }
    }

    private fun selectAfterRecord(date: LocalDate) {
        saved["selected"] = date.toString()
        state.update { it.copy(selected = date, month = YearMonth.from(date)) }
    }
}
