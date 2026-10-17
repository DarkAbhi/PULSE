package dev.abhishekan.pulse.feature.mealplan

import dev.abhishekan.pulse.core.ui.uiText
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.repository.MealPlanRepository
import dev.abhishekan.pulse.core.domain.usecase.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.time.*

@OptIn(ExperimentalCoroutinesApi::class)
class MealPlanViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private val clock = Clock.fixed(Instant.parse("2026-12-31T20:00:00Z"), ZoneOffset.UTC)
    private val calendar = MealCalendar(clock)
    private val meal = PlannedMeal(1, LocalDate.parse("2027-01-01"), "Soup", 2, "Lunch", LocalTime.NOON, LocalTime.of(14,0), false)
    private val default = MealTime(2,"Lunch",LocalTime.NOON,LocalTime.of(14,0),true)
    private inner class Repo : MealPlanRepository {
        override val changes = MutableStateFlow(0L)
        var mealFailure: Failure? = null
        var timeFailure: Failure? = null
        var meals = listOf(meal)
        var reads = 0
        var writes = 0
        var ranges = mutableListOf<Pair<LocalDate,LocalDate>>()
        var result: suspend () -> Outcome<Unit> = { Outcome.Success(Unit) }
        var toggle: Outcome<PlannedMeal> = Outcome.Success(meal.copy(consumed = true))
        override suspend fun meals(start: LocalDate, end: LocalDate): Outcome<List<PlannedMeal>> {
            reads++; ranges += start to end
            return mealFailure?.let { Outcome.Error(it) } ?: Outcome.Success(meals.filter { it.date in start..end })
        }
        override suspend fun times(): Outcome<List<MealTime>> = timeFailure?.let { Outcome.Error(it) } ?: Outcome.Success(listOf(default))
        override suspend fun apply(change: MealChange): Outcome<Unit> { writes++; return result() }
        override suspend fun consumed(id: Long, value: Boolean): Outcome<PlannedMeal> { writes++; if (toggle is Outcome.Success) meals = listOf((toggle as Outcome.Success).value); return toggle }
    }
    private val repo = Repo()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { models.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }
    private fun vm(saved: SavedStateHandle = SavedStateHandle()) = MealPlanViewModel(GetMealWeek(repo), GetMealTimes(repo), ApplyMealChange(repo), SetMealConsumed(repo),calendar,saved).also { models.put("meals",it) }
    @Test fun indiaTodayAndMondayWeeksCrossYearMonthAndLeapDay() {
        assertEquals(LocalDate.parse("2027-01-01"),calendar.today())
        listOf("2027-01-01" to "2026-12-28", "2024-03-01" to "2024-02-26", "2026-11-01" to "2026-10-26").forEach { (day,monday) -> assertEquals(LocalDate.parse(monday),calendar.monday(LocalDate.parse(day))) }
        assertNull(calendar.shift(LocalDate.of(9999,12,24),1)); assertNull(calendar.shift(LocalDate.of(1,1,1),-1))
    }
    @Test fun weekQueriesAreBoundedAndEmptyWeekIsSuccessful() = runTest {
        val vm=vm();runCurrent();assertEquals(LocalDate.parse("2026-12-28") to LocalDate.parse("2027-01-03"),repo.ranges.single())
        vm.shift(1);runCurrent();assertEquals(emptyList<PlannedMeal>(),vm.uiState.value.meals.value)
        vm.today();runCurrent();assertEquals(meal.date,vm.uiState.value.selected)
    }
    @Test fun independentFailureAndRetryKeepOtherSection() = runTest {
        repo.timeFailure=Failure.Connection;val vm=vm();runCurrent();assertEquals(listOf(meal),vm.uiState.value.meals.value)
        assertEquals(Failure.Connection,vm.uiState.value.times.error);repo.timeFailure=null;vm.refreshTimes();runCurrent();assertEquals(1,repo.reads)
        repo.mealFailure=Failure.Server;vm.refreshMeals();runCurrent();assertNotNull(vm.uiState.value.times.value);assertEquals(listOf(meal),vm.uiState.value.meals.value)
    }
    @Test fun duplicateSubmissionIsBlockedAndSuccessfulSaveKeepsFeedback() = runTest {
        val gate=CompletableDeferred<Outcome<Unit>>();repo.result={gate.await()}
        val vm=vm();runCurrent();vm.add();vm.update(vm.uiState.value.draft!!.copy(name="Rice"));vm.save();vm.save();runCurrent();assertEquals(1,repo.writes)
        gate.complete(Outcome.Success(Unit));runCurrent();assertNull(vm.uiState.value.draft);assertEquals(uiText(R.string.pulse_mealplan_saved_your_meal_plan_is_up_to_date), vm.uiState.value.message)
    }
    @Test fun failedTogglePreservesLastConfirmedValueAndRequiresSuccessfulReviewReads() = runTest {
        val vm=vm();runCurrent();repo.toggle=Outcome.Error(Failure.Timeout);repo.mealFailure=Failure.Connection
        vm.toggle(meal);vm.toggle(meal);runCurrent();assertEquals(1,repo.writes);assertFalse(vm.uiState.value.meals.value!!.single().consumed)
        assertTrue(vm.uiState.value.reviewRequired);vm.reviewed();assertTrue(vm.uiState.value.reviewRequired)
        repo.mealFailure=null;vm.refresh();runCurrent();vm.reviewed();assertFalse(vm.uiState.value.reviewRequired)
    }
    @Test fun successfulConsumptionUsesServerValueAndNeverOptimisticallyChanges() = runTest {
        val vm=vm();runCurrent();vm.toggle(meal);assertFalse(vm.uiState.value.meals.value!!.single().consumed)
        runCurrent();assertTrue(vm.uiState.value.meals.value!!.single().consumed);assertEquals(uiText(R.string.pulse_mealplan_meal_enjoyed_a_little_care_for_your_day), vm.uiState.value.message)
    }
    @Test fun savedStateRestoresDateTimeDraftSelectionAndInterruptedMutation() = runTest {
        val draft=MealDraft(timeForm=true,name="Late snack",start="23:00",end="01:00",dirty=true)
        val vm=vm(SavedStateHandle(mapOf("selected" to "2024-02-29","draft" to draft.saved(),"pending" to true,"managing" to true)))
        runCurrent();assertEquals(draft,vm.uiState.value.draft);assertEquals(LocalDate.parse("2024-02-26"),vm.uiState.value.week)
        assertTrue(vm.uiState.value.managing);assertTrue(vm.uiState.value.reviewRequired);vm.save();runCurrent();assertEquals(0,repo.writes)
    }
    @Test fun invalidDateNamesAndTimesDoNotSendRequests() = runTest {
        val vm=vm();runCurrent();vm.edit(MealDraft(name="Rice",date="2026-02-30"));vm.save();runCurrent();assertEquals(0,repo.writes);assertNotNull(vm.uiState.value.error)
        vm.discard();vm.edit(MealDraft(timeForm=true,name="Snack",start="24:00"));vm.save();runCurrent();assertEquals(0,repo.writes)
        val use=ApplyMealChange(repo)
        assertFalse(use.valid(MealChange.Save(null,meal.date," ",null)));assertFalse(use.valid(MealChange.CreateTime("x".repeat(101),LocalTime.NOON,LocalTime.MIDNIGHT)))
        assertTrue(use.valid(MealChange.CreateTime("夜".repeat(100),LocalTime.of(23,0),LocalTime.of(1,0))))
        assertTrue(use.valid(MealChange.CreateTime("Equal",LocalTime.NOON,LocalTime.NOON)))
    }
    @Test fun defaultsAreProtectedAndAssignedTimeCannotBeCleared() = runTest {
        val vm=vm();runCurrent();vm.mutate(MealChange.DeleteTime(default));runCurrent();assertEquals(0,repo.writes)
        vm.edit(MealDraft.edit(meal));vm.update(vm.uiState.value.draft!!.copy(timeId=null));vm.save();runCurrent();assertEquals(0,repo.writes)
        assertEquals(Outcome.Error(Failure.InvalidInput),SetMealConsumed(repo)(0,true))
    }
    @Test fun failedSaveKeepsDraftAndBlocksWeekChangeUntilReview() = runTest {
        repo.result={Outcome.Error(Failure.Server)};val vm=vm();runCurrent();vm.add();vm.update(vm.uiState.value.draft!!.copy(name="Rice"));vm.save();runCurrent()
        val before=vm.uiState.value.selected;vm.shift(1);vm.save();runCurrent();assertEquals(before,vm.uiState.value.selected);assertEquals(1,repo.writes);assertNotNull(vm.uiState.value.draft)
    }
    @Test fun uncertainMealOnAnotherWeekRefreshesItsTargetAndPersistsSelection() = runTest {
        repo.result={Outcome.Error(Failure.Timeout)};val saved=SavedStateHandle();val vm=vm(saved);runCurrent()
        vm.edit(MealDraft(name="Future soup",date="2027-02-10"));vm.save();runCurrent()
        assertEquals(LocalDate.parse("2027-02-08"),vm.uiState.value.week)
        assertEquals("2027-02-10",saved.get<String>("selected"));assertEquals(LocalDate.parse("2027-02-08"),repo.ranges.last().first)
        assertTrue(vm.uiState.value.reviewRequired)
    }
    @Test fun outOfRangeWeekNeverReadsRepository() = runTest {
        assertEquals(Outcome.Error(Failure.InvalidInput),GetMealWeek(repo)(LocalDate.of(9999,12,31)));assertEquals(0,repo.reads)
    }
}
