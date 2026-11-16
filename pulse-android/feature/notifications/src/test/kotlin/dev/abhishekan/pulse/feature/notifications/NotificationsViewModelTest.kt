package dev.abhishekan.pulse.feature.notifications

import androidx.lifecycle.SavedStateHandle
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Notification
import dev.abhishekan.pulse.core.domain.model.NotificationTarget
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.NotificationsRepository
import dev.abhishekan.pulse.core.domain.usecase.DismissNotification
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class Repo : NotificationsRepository {
        var items =
            listOf(
                Notification(1, "Care", null, null, targetPath = "/garage/42"),
                Notification(2, "Info", null, null, targetPath = "https://outside.example.test"),
            )
        var failure: Failure? = null
        var loadFailure: Failure? = null
        var reads = 0
        val dismissedIds = mutableListOf<Long>()
        var clears = 0
        var readGate: CompletableDeferred<Unit>? = null
        var writeGate: CompletableDeferred<Unit>? = null

        override suspend fun list(): Outcome<List<Notification>> {
            reads++
            readGate?.await()
            return loadFailure?.let { Outcome.Error(it) } ?: Outcome.Success(items)
        }

        override suspend fun dismiss(id: Long): Outcome<Unit> {
            dismissedIds += id
            writeGate?.await()
            failure?.let { return Outcome.Error(it) }
            items = items.filterNot { it.id == id }
            return Outcome.Success(Unit)
        }

        override suspend fun clear(): Outcome<Unit> {
            clears++
            failure?.let { return Outcome.Error(it) }
            items = emptyList()
            return Outcome.Success(Unit)
        }
    }

    private fun viewModel(
        repo: Repo,
        saved: SavedStateHandle = SavedStateHandle(),
    ) = NotificationsViewModel(repo, DismissNotification(repo), saved)

    @Test
    fun loadingFailureAndRetryDoNotInventAnEmptyHistory() =
        runTest {
            val repo = Repo().apply { loadFailure = Failure.Connection }
            val vm = viewModel(repo)
            vm.onAction(NotificationsAction.Refresh)
            vm.onAction(NotificationsAction.PullToRefresh)
            runCurrent()

            assertEquals(1, repo.reads)
            assertNull(vm.uiState.value.confirmedRows)
            assertEquals(NotificationsOperation.RefreshFailed(Failure.Connection), vm.uiState.value.operation)
            assertFalse(vm.uiState.value.canChange)

            repo.loadFailure = null
            vm.onAction(NotificationsAction.PullToRefresh)
            assertEquals(NotificationsOperation.Refreshing(fromPull = true), vm.uiState.value.operation)
            runCurrent()

            assertEquals(NotificationsOperation.Idle, vm.uiState.value.operation)
            assertEquals(
                listOf(1L, 2L),
                vm.uiState.value.rows
                    .map { it.id },
            )
            assertEquals(
                NotificationTarget.Vehicle(42),
                vm.uiState.value.rows
                    .first()
                    .target,
            )
            assertNull(
                vm.uiState.value.rows
                    .last()
                    .target,
            )
        }

    @Test
    fun confirmedDismissalStaysAppliedWhenFollowUpRefreshFails() =
        runTest {
            val repo = Repo()
            val saved = SavedStateHandle()
            val vm = viewModel(repo, saved)
            vm.onAction(NotificationsAction.Refresh)
            runCurrent()

            val readGate = CompletableDeferred<Unit>()
            repo.readGate = readGate
            repo.loadFailure = Failure.Connection
            vm.onAction(NotificationsAction.Dismiss(1))
            runCurrent()

            assertEquals(NotificationsOperation.Refreshing(fromPull = false), vm.uiState.value.operation)
            assertEquals(
                listOf(2L),
                vm.uiState.value.rows
                    .map { it.id },
            )
            assertNull(saved.get<Boolean>("pending"))
            vm.onAction(NotificationsAction.ClearAll)
            vm.onAction(NotificationsAction.PullToRefresh)
            runCurrent()
            assertEquals(2, repo.reads)
            assertEquals(0, repo.clears)

            readGate.complete(Unit)
            runCurrent()
            assertEquals(NotificationsOperation.RefreshFailed(Failure.Connection), vm.uiState.value.operation)
            assertEquals(
                listOf(2L),
                vm.uiState.value.rows
                    .map { it.id },
            )
            assertTrue(vm.uiState.value.canChange)
        }

    @Test
    fun openingOnlyEmitsForCurrentRoutableRowsAndDoesNotReplay() =
        runTest {
            val repo = Repo()
            val vm = viewModel(repo)
            vm.onAction(NotificationsAction.Refresh)
            runCurrent()

            val events = mutableListOf<NotificationsEvent>()
            val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.toList(events) }
            vm.onAction(NotificationsAction.Open(2))
            vm.onAction(NotificationsAction.Open(999))
            vm.onAction(NotificationsAction.Open(1))
            assertEquals(listOf(NotificationsEvent.OpenTarget(NotificationTarget.Vehicle(42))), events)
            collector.cancel()

            val laterEvents = mutableListOf<NotificationsEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.toList(laterEvents) }
            assertTrue(laterEvents.isEmpty())
            assertTrue(repo.dismissedIds.isEmpty())
        }

    @Test
    fun duplicateAndConflictingMutationsAreBlocked() =
        runTest {
            val writeGate = CompletableDeferred<Unit>()
            val repo = Repo().apply { this.writeGate = writeGate }
            val vm = viewModel(repo)
            vm.onAction(NotificationsAction.Refresh)
            runCurrent()

            vm.onAction(NotificationsAction.Dismiss(1))
            vm.onAction(NotificationsAction.Dismiss(1))
            vm.onAction(NotificationsAction.ClearAll)
            vm.onAction(NotificationsAction.Refresh)
            runCurrent()
            assertEquals(listOf(1L), repo.dismissedIds)
            assertEquals(0, repo.clears)
            assertEquals(1, repo.reads)

            writeGate.complete(Unit)
            runCurrent()
            assertEquals(
                listOf(2L),
                vm.uiState.value.rows
                    .map { it.id },
            )
            assertEquals(2, repo.reads)

            vm.onAction(NotificationsAction.Dismiss(1))
            vm.onAction(NotificationsAction.ClearAll)
            runCurrent()
            assertEquals(listOf(1L), repo.dismissedIds)
            assertEquals(1, repo.clears)
            assertTrue(
                vm.uiState.value.rows
                    .isEmpty(),
            )
        }

    @Test
    fun uncertainMutationRequiresSuccessfulRefreshBeforeAcknowledgement() =
        runTest {
            val repo = Repo()
            val saved = SavedStateHandle()
            val vm = viewModel(repo, saved)
            vm.onAction(NotificationsAction.Refresh)
            runCurrent()

            repo.failure = Failure.Timeout
            repo.loadFailure = Failure.Connection
            vm.onAction(NotificationsAction.Dismiss(1))
            runCurrent()
            assertEquals(
                listOf(1L, 2L),
                vm.uiState.value.rows
                    .map { it.id },
            )
            assertEquals(NotificationsReview.NeedsRefresh, vm.uiState.value.review)
            assertEquals(NotificationsOperation.RefreshFailed(Failure.Connection), vm.uiState.value.operation)
            assertFalse(vm.uiState.value.canAcknowledge)
            assertEquals(true, saved.get<Boolean>("pending"))

            vm.onAction(NotificationsAction.AcknowledgeReview)
            vm.onAction(NotificationsAction.Dismiss(1))
            runCurrent()
            assertEquals(listOf(1L), repo.dismissedIds)

            repo.loadFailure = null
            repo.items = repo.items.filterNot { it.id == 1L }
            vm.onAction(NotificationsAction.Refresh)
            runCurrent()
            assertEquals(
                listOf(2L),
                vm.uiState.value.rows
                    .map { it.id },
            )
            assertEquals(NotificationsReview.Ready, vm.uiState.value.review)
            assertFalse(vm.uiState.value.canChange)

            vm.onAction(NotificationsAction.AcknowledgeReview)
            assertEquals(NotificationsReview.None, vm.uiState.value.review)
            assertTrue(vm.uiState.value.canChange)
            assertNull(saved.get<Boolean>("pending"))
            assertEquals(listOf(1L), repo.dismissedIds)
        }

    @Test
    fun restoredPendingMutationOnlyReadsServerState() =
        runTest {
            val repo = Repo()
            val vm = viewModel(repo, SavedStateHandle(mapOf("pending" to true)))
            vm.onAction(NotificationsAction.Refresh)
            runCurrent()

            assertEquals(1, repo.reads)
            assertTrue(repo.dismissedIds.isEmpty())
            assertEquals(0, repo.clears)
            assertEquals(NotificationsReview.Ready, vm.uiState.value.review)
            assertTrue(vm.uiState.value.canAcknowledge)
            assertFalse(vm.uiState.value.canChange)
        }
}
