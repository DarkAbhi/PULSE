package dev.abhishekan.pulse.feature.mealplan.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.MealOnboarding
import dev.abhishekan.pulse.core.domain.model.MealOnboardingStatus
import dev.abhishekan.pulse.core.domain.model.MealPreferenceChoices
import dev.abhishekan.pulse.core.domain.model.MealPreferenceTime
import dev.abhishekan.pulse.core.domain.model.MealPreferences
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.MealPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

/** Drafts survive rotation in memory, but never mark setup done or persist across process death. */
internal class MealOnboardingViewModel(
    private val repository: MealPreferencesRepository,
) : ViewModel() {
    private val state = MutableStateFlow(MealOnboardingUiState())
    val uiState = state.asStateFlow()
    private var readJob: Job? = null
    private var firstResume = true
    private val defaultMealTimes = MealPreferences().meals.associate { it.kind to it.time }
    private val disabledMealTimes = mutableMapOf<String, LocalTime>()

    init {
        refresh()
    }

    fun onAction(action: MealOnboardingAction) {
        when (action) {
            MealOnboardingAction.Resume -> resume()
            MealOnboardingAction.Refresh -> refresh()
            is MealOnboardingAction.SelectGoal -> updateDraft { it.copy(goal = action.code) }
            is MealOnboardingAction.SelectDiet -> updateDraft { it.copy(dietaryPattern = action.code) }
            is MealOnboardingAction.ToggleBarrier -> updateDraft { it.copy(barriers = it.barriers.toggle(action.code)) }
            is MealOnboardingAction.ToggleAvoidance -> updateDraft { it.copy(foodAvoidances = it.foodAvoidances.toggle(action.code)) }
            is MealOnboardingAction.SetMealEnabled -> setMealEnabled(action.kind, action.enabled)
            is MealOnboardingAction.SetMealTime -> setMealTime(action.kind, action.time)
            MealOnboardingAction.Next -> next()
            MealOnboardingAction.Back -> back()
            is MealOnboardingAction.ReviewStep -> reviewStep(action.step)
            MealOnboardingAction.EditPreferences -> editPreferences()
            MealOnboardingAction.CancelEditing -> cancelEditing()
            MealOnboardingAction.Save -> commit(skip = false)
            MealOnboardingAction.Skip -> commit(skip = true)
        }
    }

    private fun resume() {
        // The initial read already covers the route's first resume.
        if (!firstResume) refresh()
        firstResume = false
    }

    private fun refresh() {
        if (state.value.saving) return
        readJob?.cancel()
        state.update { it.copy(error = null) }
        readJob =
            viewModelScope.launch {
                when (val result = repository.onboarding()) {
                    is Outcome.Success ->
                        state.update {
                            it.copy(record = result.value, error = null)
                        }
                    is Outcome.Error -> state.update { it.copy(error = result.failure) }
                }
            }
    }

    private fun List<String>.toggle(code: String): List<String> = if (code in this) this - code else this + code

    private fun updateDraft(update: (MealPreferences) -> MealPreferences) {
        state.update {
            if (!it.saving && it.showJourney) {
                it.copy(draft = update(it.draft), invalidStep = null, error = null)
            } else {
                it
            }
        }
    }

    private fun setMealEnabled(
        kind: String,
        enabled: Boolean,
    ) {
        val current = state.value
        if (current.saving || !current.showJourney) return
        val defaultTime = defaultMealTimes[kind] ?: return
        val existing = current.draft.meals.firstOrNull { it.kind == kind }
        val meals =
            when {
                enabled && existing == null ->
                    current.draft.meals + MealPreferenceTime(kind, disabledMealTimes.remove(kind) ?: defaultTime)
                !enabled && existing != null -> {
                    disabledMealTimes[kind] = existing.time
                    current.draft.meals - existing
                }
                else -> return
            }
        updateDraft {
            it.copy(
                meals = meals.sortedBy { MealPreferenceChoices.meals.indexOf(it.kind) },
                planningSlotsConfigured = true,
            )
        }
    }

    private fun setMealTime(
        kind: String,
        time: LocalTime,
    ) {
        updateDraft { draft ->
            draft.copy(meals = draft.meals.map { if (it.kind == kind) it.copy(time = time) else it })
        }
    }

    private fun next() {
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

    private fun back() {
        if (state.value.saving) return
        if (state.value.step == 0 && state.value.editing) {
            cancelEditing()
        } else {
            state.update { it.copy(step = (it.step - 1).coerceAtLeast(0), returnToReview = false, invalidStep = null, error = null) }
        }
    }

    private fun reviewStep(step: Int) {
        if (!state.value.saving && step in 0..5) state.update { it.copy(step = step, returnToReview = true, invalidStep = null) }
    }

    private fun editPreferences() {
        if (state.value.record == null || state.value.showJourney) return
        disabledMealTimes.clear()
        state.update {
            it.copy(
                editing = true,
                draft = it.record?.preferences ?: MealPreferences(),
                step = 0,
                error = null,
                invalidStep = null,
                returnToReview = false,
                resolvedElsewhere = false,
            )
        }
    }

    private fun cancelEditing() {
        if (state.value.saving) return
        disabledMealTimes.clear()
        state.update { it.copy(editing = false, error = null, step = 0, returnToReview = false) }
    }

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
                        state.update { it.copy(saving = false, error = result.failure) }
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
        disabledMealTimes.clear()
        val expected = if (skip) MealOnboardingStatus.Skipped else MealOnboardingStatus.Completed
        state.update {
            it.copy(
                record = record,
                saving = false,
                editing = false,
                error = null,
                resolvedElsewhere =
                    !before.editing && record.status != expected,
            )
        }
    }
}
