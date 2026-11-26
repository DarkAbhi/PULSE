package dev.abhishekan.pulse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.appearance.AppearanceUiState
import dev.abhishekan.pulse.core.domain.model.SessionState
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.session.CheckingSession
import dev.abhishekan.pulse.session.SessionAction
import dev.abhishekan.pulse.session.SessionUnavailable
import dev.abhishekan.pulse.session.SessionViewModel
import dev.abhishekan.pulse.session.SignedOutApp
import dev.abhishekan.pulse.shell.SignedInApp
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun PulseApp(
    appearance: AppearanceUiState,
    onTheme: (ThemePreference) -> Unit,
    viewModel: SessionViewModel = koinViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onAction(SessionAction.CheckExpiry) }
    val session by viewModel.session.collectAsStateWithLifecycle()
    when (val current = session) {
        SessionState.Checking -> CheckingSession()
        is SessionState.Unavailable -> {
            val operation by viewModel.operation.collectAsStateWithLifecycle()
            SessionUnavailable(
                failure = current.failure,
                busy = operation.running,
                onRetry = { viewModel.onAction(SessionAction.Retry) },
                onForget = { viewModel.onAction(SessionAction.Forget) },
            )
        }
        is SessionState.SignedOut -> SignedOutApp(current)
        is SessionState.Authenticated ->
            key(current.username) {
                val operation by viewModel.operation.collectAsStateWithLifecycle()
                val unsavedChanges by viewModel.unsavedChanges.collectAsStateWithLifecycle()
                SignedInApp(
                    username = current.username,
                    appearance = appearance,
                    onTheme = onTheme,
                    operation = operation,
                    onLogout = { viewModel.onAction(SessionAction.LogOut) },
                    clearError = { viewModel.onAction(SessionAction.ClearError) },
                    unsavedChanges = unsavedChanges,
                )
            }
    }
}
