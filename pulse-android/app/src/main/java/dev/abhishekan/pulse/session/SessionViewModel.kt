package dev.abhishekan.pulse.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.SessionState
import dev.abhishekan.pulse.core.domain.repository.AuthRepository
import dev.abhishekan.pulse.core.ui.UnsavedChanges
import dev.abhishekan.pulse.core.ui.message
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

internal class SessionViewModel(
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

    fun onAction(action: SessionAction) {
        when (action) {
            SessionAction.Retry -> retry()
            SessionAction.CheckExpiry -> viewModelScope.launch { auth.checkExpiry() }
            SessionAction.LogOut -> perform { auth.logout() }
            SessionAction.Forget -> perform { auth.forget() }
            SessionAction.ClearError -> mutableOperation.update { it.copy(error = null) }
        }
    }

    private fun retry() {
        if (restoreJob?.isActive == true) return
        restoreJob = viewModelScope.launch { auth.restore() }
    }

    private fun perform(action: suspend () -> Outcome<Unit>) {
        if (mutableOperation.value.running) return
        mutableOperation.value = SessionOperation(running = true)
        viewModelScope.launch {
            val result = action()
            // Session recovery and sign-in already get their messages from the repository's session state.
            val error = if (result is Outcome.Error && session.value is SessionState.Authenticated) result.failure.message else null
            mutableOperation.value = SessionOperation(error = error)
        }
    }
}
