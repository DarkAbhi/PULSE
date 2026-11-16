package dev.abhishekan.pulse.feature.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.abhishekan.pulse.core.domain.model.NotificationTarget
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.components.PulseScreenScaffold
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun NotificationsRoute(
    onOpen: (NotificationTarget) -> Unit,
    viewModel: NotificationsViewModel = koinViewModel(),
    backLabel: String? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val timeZone by UserTimeZone.changes.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val openTarget by rememberUpdatedState(onOpen)

    LaunchedEffect(viewModel) { viewModel.onAction(NotificationsAction.Refresh) }
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is NotificationsEvent.OpenTarget -> openTarget(event.target)
                }
            }
        }
    }
    PulseSnackbarEffect(state.message) { viewModel.onAction(NotificationsAction.MessageShown) }

    PulseScreenScaffold(
        title = stringResource(R.string.pulse_notifications_screen_title),
        backLabel = backLabel,
        actions = {
            NotificationsTopBarActions(canClear = state.canChange) {
                viewModel.onAction(NotificationsAction.ClearAll)
            }
        },
    ) {
        NotificationsScreenContent(
            state = state,
            timeZone = timeZone,
            onAction = viewModel::onAction,
        )
    }
}
