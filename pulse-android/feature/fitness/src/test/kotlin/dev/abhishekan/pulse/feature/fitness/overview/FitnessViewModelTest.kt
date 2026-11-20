package dev.abhishekan.pulse.feature.fitness.overview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.FitnessOverview
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.WorkoutSummary
import dev.abhishekan.pulse.core.domain.usecase.RecordWorkout
import dev.abhishekan.pulse.feature.fitness.shared.FitnessTestRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class FitnessViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private val repo = FitnessTestRepository()
    private val clock = Clock.fixed(Instant.parse("2026-10-17T02:00:00Z"), ZoneOffset.UTC)

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun cleanup() {
        models.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    private fun vm(
        saved: SavedStateHandle = SavedStateHandle(),
        clock: Clock = this.clock,
    ) = FitnessViewModel(
        repo,
        RecordWorkout(repo),
        saved,
        clock,
    ).also { models.put("overview", it) }

    @Test fun consumedWorkoutFeedbackCanBeProducedAgain() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.onAction(FitnessAction.RecordWorkout)
            runCurrent()
            val message = vm.uiState.value.message
            assertTrue(message != null)
            vm.onAction(FitnessAction.DismissMessage)
            assertNull(vm.uiState.value.message)
            vm.onAction(FitnessAction.RecordWorkout)
            runCurrent()
            assertEquals(message, vm.uiState.value.message)
            assertEquals(2, repo.records)
        }

    @Test fun firstResumeDoesNotReloadButReturningRefreshesExistingData() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.onAction(FitnessAction.Resume)
            runCurrent()
            assertEquals(listOf("overview"), repo.trace)
            val previous = vm.uiState.value.overview
            val gate = CompletableDeferred<Outcome<FitnessOverview>>()
            repo.overviewRead = { gate.await() }
            vm.onAction(FitnessAction.Resume)
            runCurrent()
            assertTrue(vm.uiState.value.loading)
            assertEquals(previous, vm.uiState.value.overview)
            assertTrue(vm.uiState.value.canRecord)
            vm.onAction(FitnessAction.Refresh)
            runCurrent()
            assertEquals(listOf("overview", "overview"), repo.trace)
            gate.complete(Outcome.Error(Failure.Connection))
            runCurrent()
            assertFalse(vm.uiState.value.loading)
            assertEquals(previous, vm.uiState.value.overview)
            assertEquals(Failure.Connection, vm.uiState.value.error)
            assertFalse(vm.uiState.value.canRecord)
            val retry = CompletableDeferred<Outcome<FitnessOverview>>()
            repo.overviewRead = { retry.await() }
            vm.onAction(FitnessAction.Refresh)
            runCurrent()
            assertFalse(vm.uiState.value.canRecord)
            retry.complete(Outcome.Success(repo.overviewValue))
            runCurrent()
            assertTrue(vm.uiState.value.canRecord)
        }

    @Test fun recordingDuringRefreshCancelsTheReadAndReloadsAfterWriting() =
        runTest {
            val vm = vm()
            runCurrent()
            val refresh = CompletableDeferred<Outcome<FitnessOverview>>()
            val write = CompletableDeferred<Outcome<Unit>>()
            repo.overviewRead = { refresh.await() }
            repo.write = { write.await() }
            vm.onAction(FitnessAction.Refresh)
            runCurrent()

            vm.onAction(FitnessAction.RecordWorkout)
            runCurrent()
            assertEquals(1, repo.records)
            assertTrue(vm.uiState.value.busy)
            assertFalse(vm.uiState.value.loading)
            assertFalse(vm.uiState.value.canRecord)

            refresh.complete(Outcome.Error(Failure.Connection))
            runCurrent()
            assertNull(vm.uiState.value.error)
            repo.overviewRead = null
            write.complete(Outcome.Success(Unit))
            runCurrent()
            assertEquals(listOf("overview", "overview", "record", "overview"), repo.trace)
            assertTrue(vm.uiState.value.canRecord)
        }

    @Test fun firstResumeDuringInitialLoadDoesNotQueueAnotherRequest() =
        runTest {
            val gate = CompletableDeferred<Outcome<FitnessOverview>>()
            repo.overviewRead = { gate.await() }
            val vm = vm()
            vm.onAction(FitnessAction.Resume)
            runCurrent()
            gate.complete(Outcome.Success(repo.overviewValue))
            runCurrent()
            assertEquals(listOf("overview"), repo.trace)
        }

    @Test fun selectedDateSurvivesRecreationAndMonthNavigationCrossesYear() =
        runTest {
            val saved = SavedStateHandle()
            var vm = vm(saved)
            runCurrent()
            val selected = LocalDate.of(2025, 12, 31)
            val browsed = YearMonth.of(2026, 1)
            vm.onAction(FitnessAction.SelectDate(selected))
            vm.onAction(FitnessAction.ShiftMonth(1))
            assertEquals(selected, vm.uiState.value.selected)
            assertEquals(browsed, vm.uiState.value.month)
            vm =
                vm(
                    SavedStateHandle(
                        mapOf(
                            "selected" to saved.get<String>("selected"),
                            "browsedMonth" to saved.get<String>("browsedMonth"),
                        ),
                    ),
                )
            runCurrent()
            assertEquals(selected, vm.uiState.value.selected)
            assertEquals(browsed, vm.uiState.value.month)
            vm.onAction(FitnessAction.RecordWorkout)
            runCurrent()
            assertEquals(selected, repo.selectedDate)
        }

    @Test fun todayUsesServerTodayEndpointAndDuplicateTapsDoNotQueueWrites() =
        runTest {
            val gate = CompletableDeferred<Outcome<Unit>>()
            repo.write = { gate.await() }
            val vm = vm()
            runCurrent()
            vm.onAction(FitnessAction.RecordWorkout)
            vm.onAction(FitnessAction.RecordWorkout)
            runCurrent()
            assertEquals(1, repo.records)
            assertNull(repo.selectedDate)
            assertTrue(vm.uiState.value.busy)
            gate.complete(Outcome.Success(Unit))
            runCurrent()
            assertFalse(vm.uiState.value.busy)
            assertEquals(2L, vm.uiState.value.createdId)
            assertEquals(listOf("overview", "record", "overview"), repo.trace)
        }

    @Test fun uncertainWriteRefreshesBeforeAcknowledgmentAndBlocksRetryWhenOffline() =
        runTest {
            val saved = SavedStateHandle()
            val vm = vm(saved)
            runCurrent()
            repo.write = {
                repo.readFailure = Failure.Connection
                Outcome.Error(Failure.Timeout)
            }
            vm.onAction(FitnessAction.RecordWorkout)
            runCurrent()
            assertEquals(listOf("overview", "record", "overview"), repo.trace)
            assertTrue(vm.uiState.value.needsReview)
            assertFalse(vm.uiState.value.reviewReady)
            assertEquals(true, saved.get<Boolean>("pending"))
            vm.onAction(FitnessAction.AcknowledgeReview)
            vm.onAction(FitnessAction.RecordWorkout)
            runCurrent()
            assertEquals(1, repo.records)
            repo.readFailure = null
            vm.onAction(FitnessAction.Refresh)
            runCurrent()
            assertTrue(vm.uiState.value.reviewReady)
            vm.onAction(FitnessAction.RecordWorkout)
            runCurrent()
            assertEquals(1, repo.records)
            vm.onAction(FitnessAction.AcknowledgeReview)
            assertFalse(vm.uiState.value.needsReview)
        }

    @Test fun successfulMutationInAnotherScreenRefreshesCalendar() =
        runTest {
            val vm = vm()
            runCurrent()
            repo.overviewValue =
                repo.overviewValue.copy(workouts = listOf(WorkoutSummary(3, repo.today, "8:00 AM")))
            repo.changes.value++
            runCurrent()
            assertEquals(
                3L,
                vm.uiState.value.overview
                    ?.workouts
                    ?.single()
                    ?.id,
            )
        }

    @Test fun pendingRequestAfterProcessRecreationRequiresFreshReadAndReview() =
        runTest {
            val vm = vm(SavedStateHandle(mapOf("pending" to true)))
            runCurrent()
            assertTrue(vm.uiState.value.needsReview)
            assertTrue(vm.uiState.value.reviewReady)
            vm.onAction(FitnessAction.RecordWorkout)
            runCurrent()
            assertEquals(0, repo.records)
        }

    @Test fun todayActionDoesNothingWhenTodayIsAlreadySelected() =
        runTest {
            val vm = vm()
            runCurrent()
            val before = vm.uiState.value
            vm.onAction(FitnessAction.SelectToday)
            vm.onAction(FitnessAction.SelectToday)
            assertEquals(before, vm.uiState.value)
            runCurrent()
            assertEquals(before, vm.uiState.value)
            assertEquals(listOf("overview"), repo.trace)
        }

    @Test fun todayActionRevalidatesStaleDayAfterMidnight() =
        runTest {
            val vm = vm(clock = Clock.offset(clock, Duration.ofDays(1)))
            runCurrent()
            assertEquals(repo.today, vm.uiState.value.selected)
            val nextDay = repo.today.plusDays(1)
            repo.overviewValue = repo.overviewValue.copy(today = nextDay)
            vm.onAction(FitnessAction.SelectToday)
            runCurrent()
            assertEquals(nextDay, vm.uiState.value.selected)
            assertEquals(listOf("overview", "overview"), repo.trace)
        }

    @Test fun todayActionRevalidatesBackendDayFromAnotherSelectionEvenWithClientClockSkew() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.onAction(FitnessAction.SelectDate(repo.today.minusDays(1)))
            val nextDay = repo.today.plusDays(1)
            repo.overviewValue = repo.overviewValue.copy(today = nextDay)
            vm.onAction(FitnessAction.SelectToday)
            runCurrent()
            assertEquals(nextDay, vm.uiState.value.selected)
            vm.onAction(FitnessAction.RecordWorkout)
            runCurrent()
            assertEquals(nextDay, repo.selectedDate)
        }

    @Test fun todaySelectsImmediatelyEvenWhenRefreshFails() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.onAction(FitnessAction.SelectDate(repo.today.minusDays(1)))
            vm.onAction(FitnessAction.ShiftMonth(-1))
            val gate = CompletableDeferred<Outcome<FitnessOverview>>()
            repo.overviewRead = { gate.await() }
            vm.onAction(FitnessAction.SelectToday)
            assertEquals(repo.today, vm.uiState.value.selected)
            assertEquals(YearMonth.from(repo.today), vm.uiState.value.month)
            gate.complete(Outcome.Error(Failure.Connection))
            runCurrent()
            assertEquals(repo.today, vm.uiState.value.selected)
        }

    @Test fun slowerTodayRefreshCannotOverrideALaterDateSelection() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.onAction(FitnessAction.SelectDate(repo.today.minusDays(1)))
            val gate = CompletableDeferred<Outcome<FitnessOverview>>()
            repo.overviewRead = { gate.await() }
            vm.onAction(FitnessAction.SelectToday)
            runCurrent()
            vm.onAction(FitnessAction.SelectDate(repo.today.minusDays(3)))
            gate.complete(Outcome.Success(repo.overviewValue))
            runCurrent()
            assertEquals(repo.today.minusDays(3), vm.uiState.value.selected)
        }
}
