package dev.abhishekan.pulse.feature.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.SessionState
import dev.abhishekan.pulse.core.domain.repository.AuthRepository
import dev.abhishekan.pulse.core.domain.usecase.Login
import dev.abhishekan.pulse.core.ui.message
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
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
        override val session = MutableStateFlow<SessionState>(SessionState.SignedOut())
        var calls = 0
        var credentials: Pair<String, String>? = null
        var response = CompletableDeferred<Outcome<Unit>>()

        override suspend fun login(
            username: String,
            password: String,
        ): Outcome<Unit> {
            calls++
            credentials = username to password
            return response.await()
        }

        override suspend fun restore() { }

        override suspend fun logout() = Outcome.Success(Unit)

        override suspend fun forget() = Outcome.Success(Unit)

        override suspend fun checkExpiry() { }
    }

    private fun model(
        auth: Auth,
        handle: SavedStateHandle = SavedStateHandle(),
    ) = LoginViewModel(Login(auth), handle).also { models.put("login", it) }

    @Test fun duplicateSubmissionsAreBlockedThroughSuccessAndOnlyUsernameIsTrimmed() =
        runTest {
            val auth = Auth()
            val vm = model(auth)
            vm.onAction(LoginAction.UsernameChanged("  alice  "))
            vm.onAction(LoginAction.PasswordChanged(" exact password "))
            vm.onAction(LoginAction.Submit)
            vm.onAction(LoginAction.Submit)
            vm.onAction(LoginAction.UsernameChanged("bob"))
            vm.onAction(LoginAction.PasswordChanged("another password"))
            runCurrent()
            assertEquals(1, auth.calls)
            assertEquals("alice" to " exact password ", auth.credentials)
            assertEquals("  alice  ", vm.uiState.value.username)
            assertEquals(" exact password ", vm.uiState.value.password)
            assertEquals(LoginSubmission.Submitting, vm.uiState.value.submission)
            auth.response.complete(Outcome.Success(Unit))
            runCurrent()
            assertEquals(LoginSubmission.Succeeded, vm.uiState.value.submission)
            assertEquals("", vm.uiState.value.password)
            vm.onAction(LoginAction.UsernameChanged("bob"))
            vm.onAction(LoginAction.PasswordChanged("another password"))
            vm.onAction(LoginAction.Submit)
            runCurrent()
            assertEquals(1, auth.calls)
            assertEquals("  alice  ", vm.uiState.value.username)
            assertEquals("", vm.uiState.value.password)
            assertFalse(vm.uiState.value.canEdit)
        }

    @Test fun invalidInputAndCredentialsHaveUsefulFeedback() =
        runTest {
            val auth = Auth()
            val vm = model(auth)
            vm.onAction(LoginAction.Submit)
            runCurrent()
            assertEquals(0, auth.calls)
            assertNotNull(vm.uiState.value.usernameError)
            assertNotNull(vm.uiState.value.passwordError)
            vm.onAction(LoginAction.UsernameChanged("alice"))
            vm.onAction(LoginAction.PasswordChanged("wrong"))
            auth.response.complete(Outcome.Error(Failure.InvalidCredentials))
            vm.onAction(LoginAction.Submit)
            runCurrent()
            assertEquals(LoginSubmission.Failed(Failure.InvalidCredentials.message), vm.uiState.value.submission)
            vm.onAction(LoginAction.PasswordChanged("corrected"))
            assertEquals(LoginSubmission.Idle, vm.uiState.value.submission)
        }

    @Test fun cancelledSubmissionCanBeRetried() =
        runTest {
            val auth = Auth()
            val vm = model(auth)
            vm.onAction(LoginAction.UsernameChanged("alice"))
            vm.onAction(LoginAction.PasswordChanged("password"))
            vm.onAction(LoginAction.Submit)
            runCurrent()
            assertTrue(vm.uiState.value.isSubmitting)

            auth.response.completeExceptionally(CancellationException("Cancelled request"))
            runCurrent()
            assertEquals(LoginSubmission.Idle, vm.uiState.value.submission)

            auth.response = CompletableDeferred(Outcome.Success(Unit))
            vm.onAction(LoginAction.Submit)
            runCurrent()
            assertEquals(2, auth.calls)
            assertEquals("", vm.uiState.value.password)
            assertEquals(LoginSubmission.Succeeded, vm.uiState.value.submission)
        }

    @Test fun savedStateKeepsUsernameButNeverPassword() =
        runTest {
            val auth = Auth()
            val handle = SavedStateHandle()
            val vm = model(auth, handle)
            vm.onAction(LoginAction.UsernameChanged("alice"))
            vm.onAction(LoginAction.PasswordChanged("secret"))
            assertEquals("secret", vm.uiState.value.password)
            assertFalse(handle.keys().contains("password"))
            val restored =
                model(
                    auth,
                    SavedStateHandle(
                        mapOf(
                            "username" to handle.get<String>("username"),
                        ),
                    ),
                )
            assertEquals("alice", restored.uiState.value.username)
            assertEquals("", restored.uiState.value.password)
        }
}
