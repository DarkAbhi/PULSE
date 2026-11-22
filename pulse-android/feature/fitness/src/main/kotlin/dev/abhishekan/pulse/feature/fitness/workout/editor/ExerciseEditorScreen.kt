package dev.abhishekan.pulse.feature.fitness.workout.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import dev.abhishekan.pulse.core.ui.patterns.PulseFormLayout
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
            PulseFormLayout(
                modifier = Modifier.widthIn(max = PulseTokens.readingWidth + PulseTokens.large * 4).imePadding(),
                scrollContentModifier = Modifier.padding(horizontal = size.pagePadding, vertical = PulseTokens.small),
                footer = {
                    PulseHorizontalDivider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = size.pagePadding, vertical = PulseTheme.spacing.compact)
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.compact),
                    ) {
                        PulseButton(
                            stringResource(CoreUiR.string.pulse_ui_cancel),
                            onCancel,
                            Modifier.weight(2f).fillMaxHeight().heightIn(min = PulseTokens.target),
                            enabled = !busy,
                            variant = PulseButtonVariant.Secondary,
                            contentPadding = PaddingValues(PulseTokens.small),
                            textAlign = TextAlign.Center,
                        )
                        PulseButton(
                            stringResource(CoreUiR.string.pulse_ui_save),
                            {
                                keyboard?.hide()
                                onAction(WorkoutAction.SaveExercise)
                            },
                            Modifier.weight(3f).fillMaxHeight().heightIn(min = PulseTokens.target),
                            enabled = editor.draft.issue == null && !state.needsReview && !busy,
                            loading = state.busy || state.loading,
                            contentPadding = PaddingValues(PulseTokens.small),
                            textAlign = TextAlign.Center,
                        )
                    }
                },
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
        }
    }
}
