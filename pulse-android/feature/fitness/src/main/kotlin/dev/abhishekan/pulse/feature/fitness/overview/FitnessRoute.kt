package dev.abhishekan.pulse.feature.fitness.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.patterns.PulseScreenScaffold
import dev.abhishekan.pulse.core.ui.patterns.PulseSnackbarEffect
import org.koin.androidx.compose.koinViewModel

@Composable
fun FitnessRoute(
    onWorkout: (Long) -> Unit,
    viewModel: FitnessViewModel = koinViewModel(),
) {
    val timeZone by UserTimeZone.changes.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel, timeZone) { viewModel.onAction(FitnessAction.Refresh) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PulseSnackbarEffect(state.message.takeUnless { state.needsReview }, { viewModel.onAction(FitnessAction.DismissMessage) })
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onAction(FitnessAction.Resume) }
    PulseScreenScaffold(title = "", showTopBar = false) {
        FitnessScreen(state, viewModel::onAction, onWorkout)
    }
}
