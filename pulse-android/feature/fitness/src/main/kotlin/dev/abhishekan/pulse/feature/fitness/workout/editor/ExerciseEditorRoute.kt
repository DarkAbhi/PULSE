package dev.abhishekan.pulse.feature.fitness.workout.editor

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseScreenScaffold
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import dev.abhishekan.pulse.feature.fitness.R
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutAction
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutViewModel
import kotlinx.coroutines.flow.mapNotNull

@Composable
fun ExerciseEditorRoute(
    viewModel: WorkoutViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    val editorFlow = remember(viewModel) { viewModel.uiState.mapNotNull { it.editor } }
    val lastEditor by editorFlow.collectAsStateWithLifecycle(initialValue = viewModel.uiState.value.editor)
    val editor = state.editor ?: lastEditor
    val finished = state.editor == null && !state.busy
    LaunchedEffect(finished) { if (finished) currentOnClose() }

    var hasResumed by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (hasResumed) viewModel.onAction(WorkoutAction.Resume) else hasResumed = true
    }

    PulseSnackbarEffect(
        state.message.takeUnless { state.needsReview || state.editor == null },
        { viewModel.onAction(WorkoutAction.DismissMessage) },
    )
    var discard by rememberSaveable { mutableStateOf(false) }
    val close = { viewModel.onAction(WorkoutAction.CloseEditor) }
    val requestClose = {
        if (!state.busy) {
            if (state.editor?.dirty == true) discard = true else close()
        }
    }
    BackHandler(enabled = state.busy || state.editor != null, onBack = requestClose)

    if (editor != null) {
        val title = if (editor.draft.exerciseId == null) R.string.pulse_fitness_editor_add_title else R.string.pulse_fitness_edit_exercise
        PulseScreenScaffold(
            title = stringResource(title),
            backEnabled = !state.busy,
            backLabel = stringResource(R.string.pulse_fitness_back_to_workout),
            onBack = requestClose,
        ) {
            ExerciseEditorScreen(
                editor,
                state,
                viewModel::onAction,
                onCancel = requestClose,
                closing = state.editor == null,
            )
        }
    }
    if (discard) {
        PulseDialog(
            stringResource(R.string.pulse_fitness_discard_exercise_edits),
            stringResource(R.string.pulse_fitness_your_unsaved_exercise_name_and_sets_will_be_lost),
            stringResource(R.string.pulse_fitness_discard_edits),
            {
                discard = false
                close()
            },
            { discard = false },
        )
    }
}
