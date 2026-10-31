package dev.abhishekan.pulse.feature.fitness
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.FitnessOverview
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.WorkoutSummary
import dev.abhishekan.pulse.core.domain.usecase.GetFitnessOverview
import dev.abhishekan.pulse.core.domain.usecase.ObserveFitnessChanges
import dev.abhishekan.pulse.core.domain.usecase.RecordWorkout
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
        GetFitnessOverview(repo),
        RecordWorkout(repo),
        ObserveFitnessChanges(repo),
        saved,
        clock,
    ).also { models.put("overview", it) }

    @Test fun consumedWorkoutFeedbackCanBeProducedAgain() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.recordSelected()
            runCurrent()
            val message = vm.uiState.value.message
            assertTrue(message != null)
            vm.consumeMessage()
            assertNull(vm.uiState.value.message)
            vm.recordSelected()
            runCurrent()
            assertEquals(message, vm.uiState.value.message)
            assertEquals(2, repo.records)
        }

    @Test fun firstResumeDoesNotReloadButReturningRefreshesExistingData() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.resume()
            runCurrent()
            assertEquals(listOf("overview"), repo.trace)
            val previous = vm.uiState.value.overview
            val gate = CompletableDeferred<Outcome<FitnessOverview>>()
            repo.overviewRead = { gate.await() }
            vm.resume()
            runCurrent()
            assertTrue(vm.uiState.value.loading)
            assertEquals(previous, vm.uiState.value.overview)
            vm.refresh()
            runCurrent()
            assertEquals(listOf("overview", "overview"), repo.trace)
            gate.complete(Outcome.Error(Failure.Connection))
            runCurrent()
            assertFalse(vm.uiState.value.loading)
            assertEquals(previous, vm.uiState.value.overview)
            assertEquals(Failure.Connection, vm.uiState.value.error)
        }

    @Test fun firstResumeDuringInitialLoadDoesNotQueueAnotherRequest() =
        runTest {
            val gate = CompletableDeferred<Outcome<FitnessOverview>>()
            repo.overviewRead = { gate.await() }
            val vm = vm()
            vm.resume()
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
            vm.select(LocalDate.of(2025, 12, 31))
            vm.shiftMonth(1)
            assertEquals(LocalDate.of(2026, 1, 1), vm.uiState.value.selected)
            vm = vm(SavedStateHandle(mapOf("selected" to saved.get<String>("selected"))))
            runCurrent()
            assertEquals(LocalDate.of(2026, 1, 1), vm.uiState.value.selected)
            vm.recordSelected()
            runCurrent()
            assertEquals(LocalDate.of(2026, 1, 1), repo.selectedDate)
        }

    @Test fun todayUsesServerTodayEndpointAndDuplicateTapsDoNotQueueWrites() =
        runTest {
            val gate = CompletableDeferred<Outcome<Unit>>()
            repo.write = { gate.await() }
            val vm = vm()
            runCurrent()
            vm.recordSelected()
            vm.recordSelected()
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
            vm.recordSelected()
            runCurrent()
            assertEquals(listOf("overview", "record", "overview"), repo.trace)
            assertTrue(vm.uiState.value.needsReview)
            assertFalse(vm.uiState.value.reviewReady)
            assertEquals(true, saved.get<Boolean>("pending"))
            vm.acknowledgeReview()
            vm.recordSelected()
            runCurrent()
            assertEquals(1, repo.records)
            repo.readFailure = null
            vm.refresh()
            runCurrent()
            assertTrue(vm.uiState.value.reviewReady)
            vm.recordSelected()
            runCurrent()
            assertEquals(1, repo.records)
            vm.acknowledgeReview()
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
            vm.recordSelected()
            runCurrent()
            assertEquals(0, repo.records)
        }

    @Test fun todayActionDoesNothingWhenTodayIsAlreadySelected() =
        runTest {
            val vm = vm()
            runCurrent()
            val before = vm.uiState.value
            vm.selectToday()
            vm.selectToday()
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
            vm.selectToday()
            runCurrent()
            assertEquals(nextDay, vm.uiState.value.selected)
            assertEquals(listOf("overview", "overview"), repo.trace)
        }

    @Test fun todayActionRevalidatesBackendDayFromAnotherSelectionEvenWithClientClockSkew() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.select(repo.today.minusDays(1))
            val nextDay = repo.today.plusDays(1)
            repo.overviewValue = repo.overviewValue.copy(today = nextDay)
            vm.selectToday()
            runCurrent()
            assertEquals(nextDay, vm.uiState.value.selected)
            vm.recordSelected()
            runCurrent()
            assertEquals(nextDay, repo.selectedDate)
        }

    @Test fun slowerTodayRefreshCannotOverrideALaterDateSelection() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.select(repo.today.minusDays(1))
            val gate = CompletableDeferred<Outcome<FitnessOverview>>()
            repo.overviewRead = { gate.await() }
            vm.selectToday()
            runCurrent()
            vm.select(repo.today.minusDays(3))
            gate.complete(Outcome.Success(repo.overviewValue))
            runCurrent()
            assertEquals(repo.today.minusDays(3), vm.uiState.value.selected)
        }
}
