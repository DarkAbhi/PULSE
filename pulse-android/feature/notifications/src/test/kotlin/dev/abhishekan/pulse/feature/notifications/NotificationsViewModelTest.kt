package dev.abhishekan.pulse.feature.notifications
import androidx.lifecycle.SavedStateHandle
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Notification
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.NotificationsRepository
import dev.abhishekan.pulse.core.domain.usecase.ClearNotifications
import dev.abhishekan.pulse.core.domain.usecase.DismissNotification
import dev.abhishekan.pulse.core.domain.usecase.GetNotificationCenter
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    @Before fun before() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After fun after() {
        Dispatchers.resetMain()
    }

    private class Repo : NotificationsRepository {
        var items = listOf(Notification(1, "Care", null, null))
        var failure: Failure? = null
        var loadFailure: Failure? = null
        var writes = 0
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun list(): Outcome<List<Notification>> = loadFailure?.let { Outcome.Error(it) } ?: Outcome.Success(items)

        override suspend fun dismiss(id: Long): Outcome<Unit> {
            writes++
            gate?.await()
            if (failure ==
                null
            ) {
                items = emptyList()
            }
            return failure?.let { Outcome.Error(it) } ?: Outcome.Success(Unit)
        }

        override suspend fun clear() = dismiss(1)
    }

    private fun vm(
        repo: Repo,
        saved: SavedStateHandle = SavedStateHandle(),
    ) = NotificationsViewModel(GetNotificationCenter(repo), DismissNotification(repo), ClearNotifications(repo), saved)

    @Test fun loadingFailureAndRetryDoNotInventAnEmptyHistory() =
        runTest {
            val repo = Repo().apply { loadFailure = Failure.Connection }
            val vm = vm(repo)
            runCurrent()
            assertFalse(vm.uiState.value.loaded)
            assertEquals(Failure.Connection, vm.uiState.value.failure)
            repo.loadFailure =
                null
            vm.refresh()
            runCurrent()
            assertEquals(1, vm.uiState.value.items.size)
        }

    @Test fun duplicateAndConflictingMutationsAreBlocked() =
        runTest {
            val repo = Repo().apply { gate = CompletableDeferred() }
            val vm = vm(repo)
            runCurrent()
            vm.dismiss(1)
            vm.clearAll()
            runCurrent()
            assertEquals(1, repo.writes)
            repo.gate!!.complete(Unit)
            runCurrent()
            assertTrue(
                vm.uiState.value.items
                    .isEmpty(),
            )
        }

    @Test fun failedMutationPreservesConfirmedItemsAndUncertaintyRequiresReview() =
        runTest {
            val repo = Repo().apply { failure = Failure.Timeout }
            val vm = vm(repo)
            runCurrent()
            vm.dismiss(1)
            runCurrent()
            assertEquals(1, vm.uiState.value.items.size)
            assertTrue(vm.uiState.value.uncertain)
            vm.dismiss(1)
            runCurrent()
            assertEquals(1, repo.writes)
            vm.acknowledge()
            assertFalse(vm.uiState.value.uncertain)
        }

    @Test fun restoredPendingMutationOnlyReadsServerState() =
        runTest {
            val repo = Repo()
            val vm = vm(repo, SavedStateHandle(mapOf("pending" to true)))
            runCurrent()
            assertEquals(0, repo.writes)
            assertTrue(vm.uiState.value.uncertain)
            assertTrue(vm.uiState.value.reviewed)
        }
}
