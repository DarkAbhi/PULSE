package dev.abhishekan.pulse.feature.mealplan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.usecase.*
import dev.abhishekan.pulse.core.ui.message
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DateTimeException
import java.time.LocalDate

class MealPlanViewModel(private val getWeek: GetMealWeek, private val getTimes: GetMealTimes, private val apply: ApplyMealChange,
    private val setConsumed: SetMealConsumed, private val calendar: MealCalendar, private val saved: SavedStateHandle) : ViewModel() {
    private val initialDay = saved.get<String>("selected")?.let(LocalDate::parse) ?: calendar.today()
    private val state = MutableStateFlow(MealPlanUiState(calendar.today(), initialDay, calendar.monday(initialDay),
        managing = saved["managing"] ?: false, draft = saved.get<ArrayList<String>>("draft")?.let(MealDraft::restore), reviewRequired = saved["pending"] ?: false))
    val uiState = state.asStateFlow()
    private var mealsJob: Job? = null
    private var timesJob: Job? = null
    init {
        refresh()
        viewModelScope.launch { getWeek.changes.drop(1).collect { if (!state.value.busy) refresh() } }
    }
    fun resume() { state.update { it.copy(today = calendar.today()) } }
    fun refresh() { refreshMeals(); refreshTimes() }
    fun refreshMeals() {
        mealsJob?.cancel()
        val week = state.value.week
        state.update { it.copy(meals = it.meals.copy(loading = true, error = null)) }
        mealsJob = viewModelScope.launch {
            when (val result = getWeek(week)) {
                is Outcome.Success -> state.update { it.copy(meals = MealSection(result.value, false)) }
                is Outcome.Error -> state.update { it.copy(meals = it.meals.copy(loading = false, error = result.failure)) }
            }
        }
    }
    fun refreshTimes() {
        timesJob?.cancel(); state.update { it.copy(times = it.times.copy(loading = true, error = null)) }
        timesJob = viewModelScope.launch {
            when (val result = getTimes()) {
                is Outcome.Success -> state.update { it.copy(times = MealSection(result.value, false)) }
                is Outcome.Error -> state.update { it.copy(times = it.times.copy(loading = false, error = result.failure)) }
            }
        }
    }
    fun select(date: LocalDate) {
        if (state.value.busy || state.value.draft != null || state.value.reviewRequired || date.year !in 1..9999) return
        val monday = calendar.monday(date)
        if (monday.year !in 1..9999 || monday.plusDays(6).year !in 1..9999) return
        val changed = monday != state.value.week
        saved["selected"] = date.toString()
        state.update { it.copy(selected = date, week = monday, meals = if (changed) MealSection() else it.meals, message = null) }
        if (changed) refreshMeals()
    }
    fun shift(weeks: Long) { calendar.shift(state.value.selected, weeks)?.let(::select) }
    fun today() { resume(); select(calendar.today()) }
    fun manage(value: Boolean) { if (state.value.draft == null && !state.value.busy) { saved["managing"] = value; state.update { it.copy(managing = value) } } }
    fun edit(draft: MealDraft) { if (state.value.canWrite && state.value.draft == null) store(draft) }
    fun add(date: LocalDate = state.value.selected) { edit(MealDraft(date = date.toString())) }
    fun update(draft: MealDraft) { if (!state.value.busy) store(draft.copy(dirty = true)) }
    private fun store(draft: MealDraft?) { saved["draft"] = draft?.saved(); state.update { it.copy(draft = draft, error = null) } }
    fun discard() { if (!state.value.busy) store(null) }
    fun save() {
        if (!state.value.canWrite) return
        val draft = state.value.draft ?: return
        val change = try { draft.change() } catch (_: DateTimeException) { null }
        val cannotClear = draft.id != null && draft.originalTimeId != null && draft.timeId == null
        if (change == null || !apply.valid(change) || cannotClear) {
            state.update { it.copy(error = if (cannotClear) "Choose another meal time; removing an assigned time is unavailable." else if (draft.timeForm) "Enter a name (1–100 characters) and valid 24-hour times." else "Check the meal name (1–255 characters), date and meal time.") }; return
        }
        mutate(change, true)
    }
    private fun begin() { saved["pending"] = true; state.update { it.copy(busy = true, error = null, message = null) } }
    fun mutate(change: MealChange, closeDraft: Boolean = false) {
        if (!state.value.canWrite) return
        if (!apply.valid(change)) { state.update { it.copy(message = "This meal time is protected, or the input is invalid.") }; return }
        if (change is MealChange.Save) {
            if (calendar.shift(change.date, 0) == null) { state.update { it.copy(error = "Choose a date whose whole week fits between years 1 and 9999.") }; return }
            // Keep the affected week available for review after a lost response or process recreation.
            saved["selected"] = change.date.toString()
            val week = calendar.monday(change.date)
            state.update { it.copy(selected = change.date, week = week, meals = if (it.week == week) it.meals else MealSection()) }
        }
        begin()
        viewModelScope.launch { finish(apply(change), closeDraft, (change as? MealChange.Save)?.date) }
    }
    fun toggle(meal: PlannedMeal) {
        if (!state.value.canWrite || state.value.meals.loading || state.value.meals.error != null) return
        begin()
        viewModelScope.launch {
            when (val result = setConsumed(meal.id, !meal.consumed)) {
                is Outcome.Success -> {
                    state.update { it.copy(meals = it.meals.copy(value = it.meals.value?.map { old -> if (old.id == meal.id) result.value else old })) }
                    finish(Outcome.Success(Unit), message = if (result.value.consumed) "✓ Meal enjoyed. A little care for your day." else "Meal marked as planned.")
                }
                is Outcome.Error -> finish(result)
            }
        }
    }
    private fun finish(result: Outcome<Unit>, closeDraft: Boolean = false, date: LocalDate? = null, message: String = "Saved. Your meal plan is up to date.") {
        when (result) {
            is Outcome.Success -> {
                saved["pending"] = false
                if (closeDraft) store(null)
                state.update { it.copy(busy = false) }
                if (date != null) select(date)
                state.update { it.copy(message = message) }
            }
            is Outcome.Error -> state.update { it.copy(busy = false, reviewRequired = true,
                message = "${result.failure.message} The update could not be confirmed. Your last confirmed values are kept while server records refresh. Review before another attempt.") }
        }
        refresh()
    }
    fun reviewed() {
        if (state.value.reviewRequired && state.value.reviewReady) {
            saved["pending"] = false
            state.update { it.copy(reviewRequired = false, message = "Records reviewed. Discard the draft if it was already saved.") }
        }
    }
}
