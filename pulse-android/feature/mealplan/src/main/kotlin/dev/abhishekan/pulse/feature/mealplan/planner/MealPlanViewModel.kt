package dev.abhishekan.pulse.feature.mealplan.planner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.MealChange
import dev.abhishekan.pulse.core.domain.model.MealPreferences
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.PlannedMeal
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.domain.repository.MealPlanRepository
import dev.abhishekan.pulse.core.domain.usecase.ApplyMealChange
import dev.abhishekan.pulse.core.domain.usecase.GetMealWeek
import dev.abhishekan.pulse.core.domain.usecase.MealCalendar
import dev.abhishekan.pulse.core.domain.usecase.SetMealConsumed
import dev.abhishekan.pulse.core.ui.DraftSource
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.UnsavedChanges
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.mealplan.R
import dev.abhishekan.pulse.feature.mealplan.editor.MealDraft
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DateTimeException
import java.time.LocalDate
import java.time.ZoneId

internal class MealPlanViewModel(
    private val getWeek: GetMealWeek,
    private val mealPlan: MealPlanRepository,
    private val apply: ApplyMealChange,
    private val setConsumed: SetMealConsumed,
    private val calendar: MealCalendar,
    private val saved: SavedStateHandle,
    unsavedChanges: UnsavedChanges = UnsavedChanges(),
) : ViewModel() {
    private val initialDay =
        saved.get<String>("selected")?.let(LocalDate::parse) ?: calendar.today()
    private val state =
        MutableStateFlow(
            MealPlanUiState(
                calendar.today(),
                initialDay,
                calendar.monday(initialDay),
                draft = saved.get<ArrayList<String>>("draft")?.let(MealDraft::restore),
                write = if (saved.get<Boolean>("pending") == true) MealWriteState.NeedsReview() else MealWriteState.Idle,
            ),
        )
    val uiState = state.asStateFlow()
    private val events = Channel<MealPlanEvent>(Channel.BUFFERED)
    val event = events.receiveAsFlow()

    private var mealsJob: Job? = null
    private var timesJob: Job? = null
    private var initialLoadZone: ZoneId? = UserTimeZone.zone
    private var refreshedRevision = getWeek.changes.value

    init {
        unsavedChanges.track(DraftSource.MealPlan, state.map { it.draft?.dirty == true }, viewModelScope)
        refresh()
        viewModelScope.launch {
            getWeek.changes.collect { revision ->
                if (revision != refreshedRevision && !state.value.busy) refresh()
            }
        }
    }

    fun onAction(action: MealPlanAction) {
        when (action) {
            is MealPlanAction.Enter -> enter(action.timeZone)
            MealPlanAction.Resume -> resume()
            MealPlanAction.Refresh -> if (!state.value.busy) refresh()
            MealPlanAction.RefreshMeals -> if (!state.value.busy) refreshMeals()
            MealPlanAction.RefreshTimes -> if (!state.value.busy) refreshTimes()
            MealPlanAction.Today -> today()
            is MealPlanAction.SelectDate -> select(action.date)
            is MealPlanAction.ShiftWeek -> shift(action.weeks)
            MealPlanAction.AddMeal -> edit(MealDraft(date = state.value.selected.toString()))
            is MealPlanAction.EditMeal -> edit(MealDraft.edit(action.meal))
            MealPlanAction.AddMealTime -> edit(MealDraft(timeForm = true))
            is MealPlanAction.UpdateDraft -> update(action.draft)
            MealPlanAction.Discard -> discard()
            is MealPlanAction.Save -> save(action.preferences)
            is MealPlanAction.DeleteMeal -> mutate(MealChange.Delete(action.id))
            is MealPlanAction.DeleteMealTime -> mutate(MealChange.DeleteTime(action.time))
            is MealPlanAction.ToggleConsumed -> toggle(action.meal)
            MealPlanAction.Reviewed -> reviewed()
        }
    }

    private fun enter(timeZone: ZoneId) {
        resume()
        if (initialLoadZone != timeZone && !state.value.busy) refresh()
        initialLoadZone = null
    }

    private fun resume() {
        state.update { it.copy(today = calendar.today()) }
    }

    private fun refresh() {
        // The read after a local write also covers that write's repository notification.
        refreshedRevision = getWeek.changes.value
        refreshMeals()
        refreshTimes()
    }

    private fun refreshMeals() {
        mealsJob?.cancel()
        val week = state.value.week
        state.update { it.copy(meals = MealSection.Loading(it.meals.value)) }
        mealsJob =
            viewModelScope.launch {
                when (val result = getWeek(week)) {
                    is Outcome.Success ->
                        state.update {
                            it.copy(meals = MealSection.Ready(result.value))
                        }
                    is Outcome.Error ->
                        state.update {
                            it.copy(meals = MealSection.Failed(it.meals.value, result.failure))
                        }
                }
            }
    }

    private fun refreshTimes() {
        timesJob?.cancel()
        state.update { it.copy(times = MealSection.Loading(it.times.value)) }
        timesJob =
            viewModelScope.launch {
                when (val result = mealPlan.times()) {
                    is Outcome.Success ->
                        state.update {
                            it.copy(times = MealSection.Ready(result.value))
                        }
                    is Outcome.Error ->
                        state.update {
                            it.copy(times = MealSection.Failed(it.times.value, result.failure))
                        }
                }
            }
    }

    private fun select(date: LocalDate) {
        if (state.value.busy ||
            state.value.draft != null ||
            state.value.reviewRequired ||
            date.year !in 1..9999
        ) {
            return
        }
        val monday = calendar.monday(date)
        if (monday.year !in 1..9999 || monday.plusDays(6).year !in 1..9999) return
        val changed = monday != state.value.week
        saved["selected"] = date.toString()
        state.update {
            it.copy(
                selected = date,
                week = monday,
                meals = if (changed) MealSection.Loading() else it.meals,
            )
        }
        if (changed) refreshMeals()
    }

    private fun shift(weeks: Long) {
        calendar.shift(state.value.selected, weeks)?.let(::select)
    }

    private fun today() {
        resume()
        select(calendar.today())
    }

    private fun edit(draft: MealDraft) {
        if (state.value.canWrite &&
            state.value.draft == null
        ) {
            store(draft)
        }
    }

    private fun update(draft: MealDraft) {
        if (!state.value.busy && state.value.draft != null) store(draft.copy(dirty = true))
    }

    private fun store(draft: MealDraft?) {
        saved["draft"] = draft?.saved()
        state.update { it.copy(draft = draft, error = null) }
    }

    private fun discard() {
        if (!state.value.busy) store(null)
    }

    private fun save(preferences: MealPreferences?) {
        val current = state.value
        if (!current.canWrite) return
        val draft = current.draft ?: return
        val change =
            try {
                draft.change()
            } catch (_: DateTimeException) {
                null
            }
        val cannotClear = draft.id != null && draft.originalTimeId != null && draft.timeId == null
        val unavailable =
            !draft.timeForm &&
                draft.timeId != null &&
                current.selectableTimes(preferences).none { it.id == draft.timeId }
        if (change == null || !apply.valid(change) || cannotClear || unavailable) {
            state.update {
                it.copy(
                    error =
                        if (cannotClear) {
                            uiText(
                                R.string.pulse_mealplan_choose_another_meal_time_removing_an_assigned_time_is,
                            )
                        } else if ((!draft.timeForm && draft.timeId == null) || unavailable) {
                            uiText(R.string.pulse_mealplan_select_meal_time)
                        } else if (draft.timeForm) {
                            uiText(R.string.pulse_mealplan_enter_a_name_1_100_characters_and_valid_24)
                        } else {
                            uiText(
                                R.string.pulse_mealplan_check_the_meal_name_1_255_characters_date_and,
                            )
                        },
                )
            }
            return
        }
        mutate(change, true)
    }

    private fun begin() {
        saved["pending"] = true
        state.update { it.copy(write = MealWriteState.Saving, error = null) }
    }

    private fun mutate(
        change: MealChange,
        closeDraft: Boolean = false,
    ) {
        if (!state.value.canWrite) return
        if (!apply.valid(change)) {
            showMessage(uiText(R.string.pulse_mealplan_this_meal_time_is_protected_or_the_input_is))
            return
        }
        if (change is MealChange.Save) {
            if (calendar.shift(change.date, 0) ==
                null
            ) {
                state.update {
                    it.copy(
                        error =
                            uiText(
                                R.string.pulse_mealplan_choose_a_date_whose_whole_week_fits_between_years,
                            ),
                    )
                }
                return
            }
            // Keep the affected week available for review after a lost response or process recreation.
            saved["selected"] = change.date.toString()
            val week = calendar.monday(change.date)
            if (week != state.value.week) mealsJob?.cancel()
            state.update {
                it.copy(
                    selected = change.date,
                    week = week,
                    meals = if (it.week == week) it.meals else MealSection.Loading(),
                )
            }
        }
        begin()
        viewModelScope.launch {
            finish(apply(change), closeDraft)
        }
    }

    private fun toggle(meal: PlannedMeal) {
        val current = state.value
        val meals = current.meals as? MealSection.Ready ?: return
        if (!current.canWrite) return
        begin()
        viewModelScope.launch {
            when (val result = setConsumed(meal.id, !meal.consumed)) {
                is Outcome.Success -> {
                    val updated = meals.value.map { if (it.id == meal.id) result.value else it }
                    state.update { it.copy(meals = MealSection.Ready(updated)) }
                    finish(
                        Outcome.Success(Unit),
                        message =
                            if (result.value.consumed) {
                                uiText(
                                    R.string.pulse_mealplan_meal_enjoyed_a_little_care_for_your_day,
                                )
                            } else {
                                uiText(R.string.pulse_mealplan_meal_marked_as_planned)
                            },
                    )
                }
                is Outcome.Error -> finish(result)
            }
        }
    }

    private fun finish(
        result: Outcome<Unit>,
        closeDraft: Boolean = false,
        message: UiText = uiText(R.string.pulse_mealplan_saved_your_meal_plan_is_up_to_date),
    ) {
        when (result) {
            is Outcome.Success -> {
                saved["pending"] = false
                if (closeDraft) saved["draft"] = null
                state.update { it.copy(write = MealWriteState.Idle, draft = if (closeDraft) null else it.draft) }
                showMessage(message)
            }
            is Outcome.Error ->
                state.update { it.copy(write = MealWriteState.NeedsReview(result.failure)) }
        }
        refresh()
    }

    private fun reviewed() {
        if (state.value.reviewReady) {
            saved["pending"] = false
            state.update { it.copy(write = MealWriteState.Idle) }
            showMessage(uiText(R.string.pulse_mealplan_records_reviewed_discard_the_draft_if_it_was_already))
        }
    }

    private fun showMessage(message: UiText) {
        viewModelScope.launch { events.send(MealPlanEvent.ShowMessage(message)) }
    }
}
