package dev.abhishekan.pulse

import dev.abhishekan.pulse.core.ui.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.SessionState
import dev.abhishekan.pulse.core.domain.usecase.CheckSessionExpiry
import dev.abhishekan.pulse.core.domain.usecase.ForgetSession
import dev.abhishekan.pulse.core.domain.usecase.Logout
import dev.abhishekan.pulse.core.domain.usecase.ObserveSession
import dev.abhishekan.pulse.core.domain.usecase.RestoreSession
import dev.abhishekan.pulse.core.ui.message
import java.time.Clock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SessionOperation(val running: Boolean = false, val error: UiText? = null)

class SessionViewModel(
    observe: ObserveSession, private val restore: RestoreSession, private val logout: Logout,
    private val forget: ForgetSession, private val checkExpiry: CheckSessionExpiry, private val clock: Clock,
) : ViewModel() {
    val session = observe()
    private val mutableOperation = MutableStateFlow(SessionOperation())
    val operation = mutableOperation.asStateFlow()
    private var restoreJob: Job? = null
    init {
        retry()
        viewModelScope.launch {
            session.collectLatest { state ->
                if (state is SessionState.Authenticated) {
                    delay(maxOf(0, state.expiresAtMillis - clock.millis()))
                    checkExpiry()
                }
            }
        }
    }
    fun retry() { if (restoreJob?.isActive != true) restoreJob = viewModelScope.launch { restore() } }
    fun checkCurrentExpiry() { viewModelScope.launch { checkExpiry() } }
    fun logOut() = perform { logout() }
    fun forgetSession() = perform { forget() }
    fun clearError() { mutableOperation.value = mutableOperation.value.copy(error = null) }
    private fun perform(action: suspend () -> Outcome<Unit>) {
        if (mutableOperation.value.running) return
        mutableOperation.value = SessionOperation(running = true)
        viewModelScope.launch {
            mutableOperation.value = when (val result = action()) {
                is Outcome.Success -> SessionOperation()
                is Outcome.Error -> SessionOperation(error = result.failure.message)
            }
        }
    }
}
