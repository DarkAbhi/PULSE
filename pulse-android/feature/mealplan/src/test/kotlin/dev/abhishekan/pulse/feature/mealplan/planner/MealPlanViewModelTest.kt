package dev.abhishekan.pulse.feature.mealplan.planner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.MealChange
import dev.abhishekan.pulse.core.domain.model.MealPreferences
import dev.abhishekan.pulse.core.domain.model.MealTime
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.PlannedMeal
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.domain.repository.MealPlanRepository
import dev.abhishekan.pulse.core.domain.usecase.ApplyMealChange
import dev.abhishekan.pulse.core.domain.usecase.GetMealWeek
import dev.abhishekan.pulse.core.domain.usecase.MealCalendar
import dev.abhishekan.pulse.core.domain.usecase.SetMealConsumed
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.mealplan.R
import dev.abhishekan.pulse.feature.mealplan.editor.MealDraft
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class MealPlanViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private val clock = Clock.fixed(Instant.parse("2026-12-31T20:00:00Z"), ZoneOffset.UTC)
    private val calendar = MealCalendar(clock)
    private val meal =
        PlannedMeal(
            1,
            LocalDate.parse("2027-01-01"),
            "Soup",
            2,
            "Lunch",
            LocalTime.NOON,
            LocalTime.of(14, 0),
            false,
        )
    private val default = MealTime(2, "Lunch", LocalTime.NOON, LocalTime.of(14, 0), true)

    private inner class Repo : MealPlanRepository {
        override val changes = MutableStateFlow(0L)
        var mealReadGate: CompletableDeferred<Unit>? = null
        var mealFailure: Failure? = null
        var timeFailure: Failure? = null
        var meals = listOf(meal)
        var reads = 0
        var timeReads = 0
        var writes = 0
        var ranges = mutableListOf<Pair<LocalDate, LocalDate>>()
        var result: suspend () -> Outcome<Unit> = { Outcome.Success(Unit) }
        var toggle: Outcome<PlannedMeal> = Outcome.Success(meal.copy(consumed = true))

        override suspend fun meals(
            start: LocalDate,
            end: LocalDate,
        ): Outcome<List<PlannedMeal>> {
            reads++
            ranges += start to end
            mealReadGate?.await()
            return mealFailure?.let { Outcome.Error(it) }
                ?: Outcome.Success(meals.filter { it.date in start..end })
        }

        override suspend fun times(): Outcome<List<MealTime>> {
            timeReads++
            return timeFailure?.let { Outcome.Error(it) } ?: Outcome.Success(listOf(default))
        }

        override suspend fun apply(change: MealChange): Outcome<Unit> {
            writes++
            return result().also { changes.value++ }
        }

        override suspend fun consumed(
            id: Long,
            value: Boolean,
        ): Outcome<PlannedMeal> {
            writes++
            if (toggle is Outcome.Success) {
                meals =
                    listOf((toggle as Outcome.Success).value)
            }
            return toggle
        }
    }

    private val repo = Repo()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun cleanup() {
        models.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    private fun vm(saved: SavedStateHandle = SavedStateHandle()) =
        MealPlanViewModel(
            GetMealWeek(repo),
            repo,
            ApplyMealChange(repo),
            SetMealConsumed(repo),
            calendar,
            saved,
        ).also {
            models.put("meals", it)
        }

    @Test fun firstEntryUsesInitialReadsAndReentryRefreshes() =
        runTest {
            val vm = vm()
            runCurrent()

            vm.onAction(MealPlanAction.Enter(UserTimeZone.zone))
            runCurrent()
            assertEquals(1, repo.reads)
            assertEquals(1, repo.timeReads)

            vm.onAction(MealPlanAction.Enter(UserTimeZone.zone))
            runCurrent()
            assertEquals(2, repo.reads)
            assertEquals(2, repo.timeReads)
        }

    @Test fun revisionAfterInitialRequestStartsRefreshesEvenBeforeCollectorStarts() =
        runTest {
            val vm = vm()
            repo.changes.value++
            runCurrent()

            assertEquals(2, repo.reads)
            assertEquals(2, repo.timeReads)
            assertTrue(vm.uiState.value.meals is MealSection.Ready)
        }

    @Test fun resumeDoesNotFetchAndRefreshPreservesMealsOnFailure() =
        runTest {
            val vm = vm()
            runCurrent()
            val previous = vm.uiState.value
            val reads = repo.reads
            vm.onAction(MealPlanAction.Resume)
            runCurrent()
            assertEquals(reads, repo.reads)
            repo.mealFailure = Failure.Connection
            vm.onAction(MealPlanAction.Refresh)
            assertTrue(vm.uiState.value.meals.loading)
            assertEquals(previous.meals.value, vm.uiState.value.meals.value)
            assertEquals(previous.times.value, vm.uiState.value.times.value)
            runCurrent()
            assertFalse(vm.uiState.value.meals.loading)
            assertEquals(Failure.Connection, vm.uiState.value.meals.error)
            assertEquals(previous.meals.value, vm.uiState.value.meals.value)
        }

    @Test fun utcTodayAndMondayWeeksCrossYearMonthAndLeapDay() {
        assertEquals(LocalDate.parse("2026-12-31"), calendar.today())
        listOf(
            "2027-01-01" to "2026-12-28",
            "2024-03-01" to "2024-02-26",
            "2026-11-01" to "2026-10-26",
        ).forEach { (day, monday) ->
            assertEquals(LocalDate.parse(monday), calendar.monday(LocalDate.parse(day)))
        }
        assertNull(calendar.shift(LocalDate.of(9999, 12, 24), 1))
        assertNull(calendar.shift(LocalDate.of(1, 1, 1), -1))
    }

    @Test fun weekQueriesAreBoundedAndEmptyWeekIsSuccessful() =
        runTest {
            val vm = vm()
            runCurrent()
            assertEquals(
                LocalDate.parse("2026-12-28") to LocalDate.parse("2027-01-03"),
                repo.ranges.single(),
            )
            vm.onAction(MealPlanAction.ShiftWeek(1))
            runCurrent()
            assertEquals(emptyList<PlannedMeal>(), vm.uiState.value.meals.value)
            vm.onAction(MealPlanAction.Today)
            runCurrent()
            assertEquals(calendar.today(), vm.uiState.value.selected)
        }

    @Test fun independentFailureAndRetryKeepOtherSection() =
        runTest {
            repo.timeFailure = Failure.Connection
            val vm = vm()
            runCurrent()
            assertEquals(listOf(meal), vm.uiState.value.meals.value)
            assertEquals(Failure.Connection, vm.uiState.value.times.error)
            repo.timeFailure = null
            vm.onAction(MealPlanAction.RefreshTimes)
            runCurrent()
            assertEquals(1, repo.reads)
            repo.mealFailure = Failure.Server
            vm.onAction(MealPlanAction.RefreshMeals)
            runCurrent()
            assertNotNull(vm.uiState.value.times.value)
            assertEquals(listOf(meal), vm.uiState.value.meals.value)
        }

    @Test fun duplicateSubmissionIsBlockedAndOwnRevisionTriggersOneRefresh() =
        runTest {
            val gate = CompletableDeferred<Outcome<Unit>>()
            repo.result = { gate.await() }
            val vm = vm()
            runCurrent()
            vm.onAction(MealPlanAction.AddMeal)
            vm.onAction(
                MealPlanAction.UpdateDraft(
                    vm.uiState.value.draft!!
                        .copy(name = "Rice", timeId = default.id),
                ),
            )
            vm.onAction(MealPlanAction.Save())
            vm.onAction(MealPlanAction.Save())
            runCurrent()
            assertEquals(1, repo.writes)
            gate.complete(Outcome.Success(Unit))
            runCurrent()
            assertNull(vm.uiState.value.draft)
            assertEquals(
                uiText(R.string.pulse_mealplan_saved_your_meal_plan_is_up_to_date),
                (vm.event.first() as MealPlanEvent.ShowMessage).message,
            )
            assertEquals(2, repo.reads)
            assertEquals(2, repo.timeReads)
        }

    @Test fun mealTimeIsRequiredBeforeSaving() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.onAction(MealPlanAction.AddMeal)
            vm.onAction(
                MealPlanAction.UpdateDraft(
                    vm.uiState.value.draft!!
                        .copy(name = "Rice"),
                ),
            )
            vm.onAction(MealPlanAction.Save())
            runCurrent()
            assertEquals(0, repo.writes)
            assertEquals(uiText(R.string.pulse_mealplan_select_meal_time), vm.uiState.value.error)
            assertNotNull(vm.uiState.value.draft)

            vm.onAction(
                MealPlanAction.UpdateDraft(
                    vm.uiState.value.draft!!
                        .copy(timeId = default.id),
                ),
            )
            vm.onAction(MealPlanAction.Save())
            runCurrent()
            assertEquals(1, repo.writes)
            assertNull(vm.uiState.value.draft)
        }

    @Test fun failedTogglePreservesLastConfirmedValueAndRequiresSuccessfulReviewReads() =
        runTest {
            val vm = vm()
            runCurrent()
            repo.toggle = Outcome.Error(Failure.Timeout)
            repo.mealFailure =
                Failure.Connection
            vm.onAction(MealPlanAction.ToggleConsumed(meal))
            vm.onAction(MealPlanAction.ToggleConsumed(meal))
            runCurrent()
            assertEquals(1, repo.writes)
            assertFalse(
                vm.uiState.value.meals.value!!
                    .single()
                    .consumed,
            )
            assertTrue(vm.uiState.value.reviewRequired)
            assertEquals(MealWriteState.NeedsReview(Failure.Timeout), vm.uiState.value.write)
            vm.onAction(MealPlanAction.Reviewed)
            assertTrue(vm.uiState.value.reviewRequired)
            repo.mealFailure = null
            vm.onAction(MealPlanAction.Refresh)
            runCurrent()
            vm.onAction(MealPlanAction.Reviewed)
            assertFalse(vm.uiState.value.reviewRequired)
        }

    @Test fun successfulConsumptionUsesServerValueAndNeverOptimisticallyChanges() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.onAction(MealPlanAction.ToggleConsumed(meal))
            assertFalse(
                vm.uiState.value.meals.value!!
                    .single()
                    .consumed,
            )
            runCurrent()
            assertTrue(
                vm.uiState.value.meals.value!!
                    .single()
                    .consumed,
            )
            assertEquals(
                uiText(R.string.pulse_mealplan_meal_enjoyed_a_little_care_for_your_day),
                (vm.event.first() as MealPlanEvent.ShowMessage).message,
            )
        }

    @Test fun savedStateRestoresDateTimeDraftSelectionAndInterruptedMutation() =
        runTest {
            val draft =
                MealDraft(
                    timeForm = true,
                    name = "Late snack",
                    start = "23:00",
                    end = "01:00",
                    dirty = true,
                )
            val vm =
                vm(
                    SavedStateHandle(
                        mapOf(
                            "selected" to "2024-02-29",
                            "draft" to draft.saved(),
                            "pending" to true,
                        ),
                    ),
                )
            runCurrent()
            assertEquals(draft, vm.uiState.value.draft)
            assertEquals(LocalDate.parse("2024-02-26"), vm.uiState.value.week)
            assertTrue(vm.uiState.value.reviewRequired)
            vm.onAction(MealPlanAction.Save())
            runCurrent()
            assertEquals(0, repo.writes)
        }

    @Test fun invalidDateNamesAndTimesDoNotSendRequests() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.onAction(MealPlanAction.AddMeal)
            vm.onAction(MealPlanAction.UpdateDraft(MealDraft(name = "Rice", date = "2026-02-30", timeId = default.id)))
            vm.onAction(MealPlanAction.Save())
            runCurrent()
            assertEquals(0, repo.writes)
            assertNotNull(vm.uiState.value.error)
            vm.onAction(MealPlanAction.Discard)
            vm.onAction(MealPlanAction.AddMealTime)
            vm.onAction(MealPlanAction.UpdateDraft(MealDraft(timeForm = true, name = "Snack", start = "24:00")))
            vm.onAction(MealPlanAction.Save())
            runCurrent()
            assertEquals(0, repo.writes)
            val use = ApplyMealChange(repo)
            listOf(null, 0L, -1L).forEach { timeId ->
                val change = MealChange.Save(null, meal.date, "Rice", timeId)
                assertFalse(use.valid(change))
                assertEquals(Outcome.Error(Failure.InvalidInput), use(change))
            }
            assertEquals(0, repo.writes)
            assertFalse(use.valid(MealChange.Save(null, meal.date, " ", null)))
            assertFalse(
                use.valid(MealChange.CreateTime("x".repeat(101), LocalTime.NOON, LocalTime.MIDNIGHT)),
            )
            assertTrue(
                use.valid(
                    MealChange.CreateTime("夜".repeat(100), LocalTime.of(23, 0), LocalTime.of(1, 0)),
                ),
            )
            assertTrue(use.valid(MealChange.CreateTime("Equal", LocalTime.NOON, LocalTime.NOON)))
        }

    @Test fun defaultsAreProtectedAndAssignedTimeCannotBeCleared() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.onAction(MealPlanAction.DeleteMealTime(default))
            runCurrent()
            assertEquals(0, repo.writes)
            vm.onAction(MealPlanAction.EditMeal(meal))
            vm.onAction(
                MealPlanAction.UpdateDraft(
                    vm.uiState.value.draft!!
                        .copy(timeId = null),
                ),
            )
            vm.onAction(MealPlanAction.Save())
            runCurrent()
            assertEquals(0, repo.writes)
            assertEquals(Outcome.Error(Failure.InvalidInput), SetMealConsumed(repo)(0, true))
        }

    @Test fun failedSaveKeepsDraftAndBlocksWeekChangeUntilReview() =
        runTest {
            repo.result = { Outcome.Error(Failure.Server) }
            val vm = vm()
            runCurrent()
            vm.onAction(MealPlanAction.AddMeal)
            vm.onAction(
                MealPlanAction.UpdateDraft(
                    vm.uiState.value.draft!!
                        .copy(name = "Rice", timeId = default.id),
                ),
            )
            vm.onAction(MealPlanAction.Save())
            runCurrent()
            val before = vm.uiState.value.selected
            vm.onAction(MealPlanAction.ShiftWeek(1))
            vm.onAction(MealPlanAction.Save())
            runCurrent()
            assertEquals(before, vm.uiState.value.selected)
            assertEquals(1, repo.writes)
            assertNotNull(vm.uiState.value.draft)
        }

    @Test fun uncertainMealOnAnotherWeekRefreshesItsTargetAndPersistsSelection() =
        runTest {
            repo.result = { Outcome.Error(Failure.Timeout) }
            val saved = SavedStateHandle()
            val vm = vm(saved)
            runCurrent()
            vm.onAction(MealPlanAction.AddMeal)
            vm.onAction(MealPlanAction.UpdateDraft(MealDraft(name = "Future soup", date = "2027-02-10", timeId = default.id)))
            vm.onAction(MealPlanAction.Save())
            runCurrent()
            assertEquals(LocalDate.parse("2027-02-08"), vm.uiState.value.week)
            assertEquals("2027-02-10", saved.get<String>("selected"))
            assertEquals(LocalDate.parse("2027-02-08"), repo.ranges.last().first)
            assertTrue(vm.uiState.value.reviewRequired)
        }

    @Test fun outOfRangeWeekNeverReadsRepository() =
        runTest {
            assertEquals(
                Outcome.Error(Failure.InvalidInput),
                GetMealWeek(repo)(LocalDate.of(9999, 12, 31)),
            )
            assertEquals(0, repo.reads)
        }

    @Test fun excludedMealTimeIsRejectedForNewMealsAndAllowedForExistingAssignment() =
        runTest {
            val vm = vm()
            val preferences = MealPreferences(meals = emptyList(), planningSlotsConfigured = true)
            runCurrent()

            vm.onAction(MealPlanAction.AddMeal)
            val draft = requireNotNull(vm.uiState.value.draft)
            vm.onAction(MealPlanAction.UpdateDraft(draft.copy(name = "Soup", timeId = default.id)))
            vm.onAction(MealPlanAction.Save(preferences))
            runCurrent()
            assertEquals(0, repo.writes)
            assertEquals(uiText(R.string.pulse_mealplan_select_meal_time), vm.uiState.value.error)

            vm.onAction(MealPlanAction.Discard)
            vm.onAction(MealPlanAction.EditMeal(meal))
            vm.onAction(MealPlanAction.Save(preferences))
            runCurrent()
            assertEquals(1, repo.writes)
        }

    @Test fun savingToAnotherWeekCancelsTheReadForThePreviousWeek() =
        runTest {
            val oldRead = CompletableDeferred<Unit>()
            val write = CompletableDeferred<Unit>()
            repo.mealReadGate = oldRead
            repo.result = {
                write.await()
                Outcome.Success(Unit)
            }
            val vm = vm()
            runCurrent()

            vm.onAction(MealPlanAction.AddMeal)
            val draft = requireNotNull(vm.uiState.value.draft)
            vm.onAction(MealPlanAction.UpdateDraft(draft.copy(name = "Soup", date = "2027-02-10", timeId = default.id)))
            vm.onAction(MealPlanAction.Save())
            runCurrent()
            oldRead.complete(Unit)
            runCurrent()

            assertEquals(LocalDate.parse("2027-02-08"), vm.uiState.value.week)
            assertNull(vm.uiState.value.meals.value)
            assertTrue(vm.uiState.value.busy)

            write.complete(Unit)
            runCurrent()
            assertEquals(emptyList<PlannedMeal>(), vm.uiState.value.meals.value)
        }
}
