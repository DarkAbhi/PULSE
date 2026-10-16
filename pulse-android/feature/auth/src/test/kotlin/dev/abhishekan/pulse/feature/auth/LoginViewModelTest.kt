package dev.abhishekan.pulse.feature.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.SessionState
import dev.abhishekan.pulse.core.domain.repository.AuthRepository
import dev.abhishekan.pulse.core.domain.usecase.Login
import dev.abhishekan.pulse.core.ui.message
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { models.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }

    private class Auth : AuthRepository {
        override val session = MutableStateFlow<SessionState>(SessionState.SignedOut())
        var calls = 0
        var credentials: Pair<String, String>? = null
        val response = CompletableDeferred<Outcome<Unit>>()
        override suspend fun login(username: String, password: String): Outcome<Unit> {
            calls++; credentials = username to password; return response.await()
        }
        override suspend fun restore() { }
        override suspend fun logout() = Outcome.Success(Unit)
        override suspend fun forget() = Outcome.Success(Unit)
        override suspend fun checkExpiry() { }
    }
    private fun model(auth: Auth, handle: SavedStateHandle = SavedStateHandle()) =
        LoginViewModel(Login(auth), handle).also { models.put("login", it) }

    @Test fun duplicateSubmissionsAreBlockedAndOnlyUsernameIsTrimmed() = runTest {
        val auth = Auth(); val vm = model(auth)
        vm.usernameChanged("  alice  "); vm.passwordChanged(" exact password ")
        vm.submit(); vm.submit(); runCurrent()
        assertEquals(1, auth.calls)
        assertEquals("alice" to " exact password ", auth.credentials)
        assertTrue(vm.uiState.value.loading)
        auth.response.complete(Outcome.Success(Unit)); runCurrent()
        assertEquals("", vm.uiState.value.password)
        assertFalse(vm.uiState.value.loading)
    }

    @Test fun invalidInputAndCredentialsHaveUsefulFeedback() = runTest {
        val auth = Auth(); val vm = model(auth)
        vm.submit(); runCurrent()
        assertEquals(0, auth.calls)
        assertNotNull(vm.uiState.value.usernameError)
        assertNotNull(vm.uiState.value.passwordError)
        vm.usernameChanged("alice"); vm.passwordChanged("wrong")
        auth.response.complete(Outcome.Error(Failure.InvalidCredentials))
        vm.submit(); runCurrent()
        assertEquals(Failure.InvalidCredentials.message, vm.uiState.value.error)
        assertFalse(vm.uiState.value.loading)
    }

    @Test fun savedStateKeepsUsernameButNeverPassword() = runTest {
        val auth = Auth(); val handle = SavedStateHandle()
        val vm = model(auth, handle)
        vm.usernameChanged("alice"); vm.passwordChanged("secret")
        assertEquals("secret", vm.uiState.value.password)
        assertFalse(handle.keys().contains("password"))
        val restored = model(auth, SavedStateHandle(mapOf("username" to handle.get<String>("username"))))
        assertEquals("alice", restored.uiState.value.username)
        assertEquals("", restored.uiState.value.password)
    }
}
