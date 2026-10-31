package dev.abhishekan.pulse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.SessionState
import dev.abhishekan.pulse.core.domain.repository.AuthRepository
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.UnsavedChanges
import dev.abhishekan.pulse.core.ui.message
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock

data class SessionOperation(
    val running: Boolean = false,
    val error: UiText? = null,
)

class SessionViewModel(
    private val auth: AuthRepository,
    private val clock: Clock,
    unsavedChanges: UnsavedChanges = UnsavedChanges(),
) : ViewModel() {
    val unsavedChanges = unsavedChanges.sources.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val session = auth.session
    private val mutableOperation = MutableStateFlow(SessionOperation())
    val operation = mutableOperation.asStateFlow()
    private var restoreJob: Job? = null

    init {
        retry()
        viewModelScope.launch {
            session.collectLatest { state ->
                if (state is SessionState.Authenticated) {
                    delay(maxOf(0, state.expiresAtMillis - clock.millis()))
                    auth.checkExpiry()
                }
            }
        }
    }

    fun retry() {
        if (restoreJob?.isActive !=
            true
        ) {
            restoreJob = viewModelScope.launch { auth.restore() }
        }
    }

    fun checkCurrentExpiry() {
        viewModelScope.launch { auth.checkExpiry() }
    }

    fun logOut() = perform { auth.logout() }

    fun forgetSession() = perform { auth.forget() }

    fun clearError() {
        mutableOperation.value = mutableOperation.value.copy(error = null)
    }

    private fun perform(action: suspend () -> Outcome<Unit>) {
        if (mutableOperation.value.running) return
        mutableOperation.value = SessionOperation(running = true)
        viewModelScope.launch {
            mutableOperation.value =
                when (val result = action()) {
                    is Outcome.Success -> SessionOperation()
                    is Outcome.Error -> SessionOperation(error = result.failure.message)
                }
        }
    }
}
