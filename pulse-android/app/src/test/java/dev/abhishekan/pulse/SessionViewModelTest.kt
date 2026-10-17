package dev.abhishekan.pulse
import androidx.lifecycle.ViewModelStore
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.SessionState
import dev.abhishekan.pulse.core.domain.repository.AuthRepository
import dev.abhishekan.pulse.core.domain.usecase.CheckSessionExpiry
import dev.abhishekan.pulse.core.domain.usecase.ForgetSession
import dev.abhishekan.pulse.core.domain.usecase.Logout
import dev.abhishekan.pulse.core.domain.usecase.ObserveSession
import dev.abhishekan.pulse.core.domain.usecase.RestoreSession
import dev.abhishekan.pulse.core.ui.message
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun cleanup() {
        models.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    private class Auth : AuthRepository {
        override val session = MutableStateFlow<SessionState>(SessionState.Checking)
        val ready = CompletableDeferred<Unit>()
        var restores = 0
        var logoutResult: Outcome<Unit> = Outcome.Error(Failure.Connection)

        override suspend fun restore() {
            restores++
            ready.await()
            session.value =
                SessionState.Authenticated("fixture", 1_000)
        }

        override suspend fun login(
            username: String,
            password: String,
        ) = Outcome.Success(Unit)

        override suspend fun logout(): Outcome<Unit> {
            if (logoutResult is Outcome.Success) session.value = SessionState.SignedOut()
            return logoutResult
        }

        override suspend fun forget() = Outcome.Success(Unit)

        override suspend fun checkExpiry() {
            session.value = SessionState.SignedOut(expired = true)
        }
    }

    private fun model(auth: Auth) =
        SessionViewModel(
            ObserveSession(auth),
            RestoreSession(auth),
            Logout(auth),
            ForgetSession(auth),
            CheckSessionExpiry(auth),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
        ).also {
            models.put("session", it)
        }

    @Test fun restorationIsGatedAndConfirmedExpiryUpdatesSession() =
        runTest {
            val auth = Auth()
            val vm = model(auth)
            vm.retry()
            vm.retry()
            runCurrent()
            assertEquals(1, auth.restores)
            assertEquals(SessionState.Checking, vm.session.value)
            auth.ready.complete(Unit)
            runCurrent()
            assertTrue(vm.session.value is SessionState.Authenticated)
            advanceTimeBy(1_001)
            runCurrent()
            assertEquals(SessionState.SignedOut(expired = true), vm.session.value)
        }

    @Test fun logoutFailureIsVisibleAndCanBeRetried() =
        runTest {
            val auth = Auth()
            auth.ready.complete(Unit)
            val vm = model(auth)
            runCurrent()
            vm.logOut()
            runCurrent()
            assertTrue(vm.session.value is SessionState.Authenticated)
            assertEquals(Failure.Connection.message, vm.operation.value.error)
            auth.logoutResult = Outcome.Success(Unit)
            vm.logOut()
            runCurrent()
            assertEquals(SessionState.SignedOut(), vm.session.value)
            assertNull(vm.operation.value.error)
        }
}
