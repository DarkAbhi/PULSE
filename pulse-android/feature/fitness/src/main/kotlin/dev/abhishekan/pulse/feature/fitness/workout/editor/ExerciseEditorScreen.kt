package dev.abhishekan.pulse.feature.fitness.workout.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseFormAction
import dev.abhishekan.pulse.core.ui.components.PulseFormActions
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.feature.fitness.shared.MutationReview
import dev.abhishekan.pulse.feature.fitness.shared.fitnessFailureMessage
import dev.abhishekan.pulse.feature.fitness.workout.ServerExercises
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutAction
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutUiState
import dev.abhishekan.pulse.core.ui.R as CoreUiR

@Composable
fun ExerciseEditorScreen(
    editor: ExerciseEditorState,
    state: WorkoutUiState,
    onAction: (WorkoutAction) -> Unit,
    onCancel: () -> Unit,
    closing: Boolean = false,
) {
    val busy = state.busy || state.loading || closing
    val keyboard = LocalSoftwareKeyboardController.current
    PulseAdaptiveLayout(Modifier.fillMaxSize()) { size ->
        Box(
            Modifier.fillMaxSize().padding(LocalPulseContentPadding.current),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .widthIn(max = PulseTokens.readingWidth + PulseTokens.large * 4)
                    .fillMaxSize()
                    .imePadding(),
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(size.pagePadding),
                    verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
                ) {
                    val refresh = { onAction(WorkoutAction.Refresh) }
                    state.error?.let { PulseErrorState(fitnessFailureMessage(it), refresh) }
                    if (state.needsReview) {
                        MutationReview(
                            state.reviewReady,
                            state.busy,
                            refresh,
                            { onAction(WorkoutAction.AcknowledgeReview) },
                            message = state.message,
                        )
                        state.detail?.let { ServerExercises(it.exercises) }
                    }
                    ExerciseEditor(editor, busy, onAction)
                }
                PulseFormActions(
                    primary =
                        PulseFormAction(
                            stringResource(CoreUiR.string.pulse_ui_save),
                            {
                                keyboard?.hide()
                                onAction(WorkoutAction.SaveExercise)
                            },
                            enabled = editor.draft.issue == null && !state.needsReview && !busy,
                            loading = state.busy || state.loading,
                        ),
                    secondary =
                        PulseFormAction(
                            stringResource(CoreUiR.string.pulse_ui_cancel),
                            onCancel,
                            enabled = !busy,
                        ),
                    modifier = Modifier.padding(horizontal = size.pagePadding),
                )
            }
        }
    }
}
