package dev.abhishekan.pulse.feature.fitness.workout

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.patterns.PulseScreenScaffold
import dev.abhishekan.pulse.core.ui.patterns.PulseSnackbarEffect
import dev.abhishekan.pulse.feature.fitness.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun WorkoutRoute(
    onExercise: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: WorkoutViewModel = koinViewModel(),
    backLabel: String? = null,
) {
    val timeZone by UserTimeZone.changes.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel, timeZone) { viewModel.onAction(WorkoutAction.Refresh) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PulseSnackbarEffect(
        state.message.takeUnless { state.needsReview || state.editor != null },
        { viewModel.onAction(WorkoutAction.DismissMessage) },
    )
    BackHandler(enabled = state.busy && state.editor == null) {}
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (viewModel.uiState.value.editor
                ?.dirty == true
        ) {
            onExercise()
        } else {
            viewModel.onAction(WorkoutAction.CloseEditor)
        }
        viewModel.onAction(WorkoutAction.Resume)
    }
    val currentOnDeleted by rememberUpdatedState(onDeleted)
    LaunchedEffect(state.deleted) { if (state.deleted) currentOnDeleted() }
    PulseScreenScaffold(
        title = stringResource(R.string.pulse_fitness_workout_title),
        backEnabled = !state.busy,
        backLabel = backLabel,
    ) {
        WorkoutScreen(
            state = state,
            onAction = viewModel::onAction,
            onExercise = { id ->
                if (viewModel.uiState.value.editor == null) {
                    viewModel.onAction(WorkoutAction.EditExercise(id))
                    if (viewModel.uiState.value.editor != null) onExercise()
                }
            },
            timeZone = timeZone,
        )
    }
}
