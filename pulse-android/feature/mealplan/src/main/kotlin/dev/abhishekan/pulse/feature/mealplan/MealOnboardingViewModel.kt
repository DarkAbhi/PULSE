package dev.abhishekan.pulse.feature.mealplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.MealOnboarding
import dev.abhishekan.pulse.core.domain.model.MealOnboardingStatus
import dev.abhishekan.pulse.core.domain.model.MealPreferences
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.MealPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MealOnboardingUiState(
    val record: MealOnboarding? = null,
    val loading: Boolean = true,
    val error: Failure? = null,
    val draft: MealPreferences = MealPreferences(),
    val step: Int = 0,
    val editing: Boolean = false,
    val saving: Boolean = false,
    val invalidStep: Int? = null,
    val returnToReview: Boolean = false,
    val resolvedElsewhere: Boolean = false,
) {
    val showJourney get() = record?.status == MealOnboardingStatus.Pending || editing
}

/** Drafts survive rotation in memory, but never mark setup done or persist across process death. */
class MealOnboardingViewModel(
    private val repository: MealPreferencesRepository,
) : ViewModel() {
    private val state = MutableStateFlow(MealOnboardingUiState())
    val uiState = state.asStateFlow()
    private var readJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (state.value.saving) return
        readJob?.cancel()
        state.update { it.copy(loading = true, error = null) }
        readJob =
            viewModelScope.launch {
                when (val result = repository.onboarding()) {
                    is Outcome.Success ->
                        state.update {
                            it.copy(record = result.value, loading = false, error = null)
                        }
                    is Outcome.Error -> state.update { it.copy(loading = false, error = result.failure) }
                }
            }
    }

    fun update(draft: MealPreferences) {
        if (!state.value.saving) state.update { it.copy(draft = draft, invalidStep = null, error = null) }
    }

    fun next() {
        val current = state.value
        if (current.saving) return
        val invalid = current.draft.invalidStep()
        if (invalid == current.step) {
            state.update { it.copy(invalidStep = invalid) }
            return
        }
        state.update {
            it.copy(
                step = if (it.returnToReview) 6 else (it.step + 1).coerceAtMost(6),
                returnToReview = false,
                invalidStep = null,
                error = null,
            )
        }
    }

    fun back() {
        if (state.value.saving) return
        if (state.value.step == 0 && state.value.editing) {
            cancelEditing()
        } else {
            state.update { it.copy(step = (it.step - 1).coerceAtLeast(0), returnToReview = false, invalidStep = null, error = null) }
        }
    }

    fun reviewStep(step: Int) {
        if (!state.value.saving && step in 0..5) state.update { it.copy(step = step, returnToReview = true, invalidStep = null) }
    }

    fun editPreferences() {
        if (state.value.record == null || state.value.showJourney) return
        state.update {
            it.copy(
                editing = true,
                draft = it.record?.preferences ?: MealPreferences(),
                step = 0,
                error = null,
                invalidStep = null,
                resolvedElsewhere = false,
            )
        }
    }

    fun cancelEditing() {
        if (!state.value.saving) state.update { it.copy(editing = false, error = null, step = 0, returnToReview = false) }
    }

    fun save() = commit(false)

    fun skip() = commit(true)

    private fun commit(skip: Boolean) {
        val before = state.value
        if (before.saving || before.record == null || !before.showJourney || (skip && before.editing)) return
        if (!skip) {
            before.draft.invalidStep()?.let { invalid ->
                state.update { it.copy(step = invalid, invalidStep = invalid) }
                return
            }
        }
        readJob?.cancel()
        state.update { it.copy(saving = true, error = null, invalidStep = null) }
        viewModelScope.launch {
            val result = if (before.editing) repository.save(before.draft) else repository.resolve(if (skip) null else before.draft)
            when (result) {
                is Outcome.Success -> accept(result.value, before, skip)
                is Outcome.Error -> {
                    // A timeout can follow a successful server commit. A fresh read
                    // is authoritative on either platform; never create a local flag.
                    val current = if (result.failure == Failure.Unauthorized) null else repository.onboarding()
                    if (current is Outcome.Success &&
                        current.value.status != MealOnboardingStatus.Pending &&
                        (!before.editing || current.value.preferences == before.draft)
                    ) {
                        accept(current.value, before, skip)
                    } else {
                        state.update { it.copy(saving = false, loading = false, error = result.failure) }
                    }
                }
            }
        }
    }

    private fun accept(
        record: MealOnboarding,
        before: MealOnboardingUiState,
        skip: Boolean,
    ) {
        val expected = if (skip) MealOnboardingStatus.Skipped else MealOnboardingStatus.Completed
        state.update {
            it.copy(
                record = record,
                saving = false,
                loading = false,
                editing = false,
                error = null,
                resolvedElsewhere =
                    !before.editing && record.status != expected,
            )
        }
    }
}
