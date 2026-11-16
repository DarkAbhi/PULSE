package dev.abhishekan.pulse.feature.fitness.workout.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.feature.fitness.shared.FitnessLoadState
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutUiState

@Preview(name = "Exercise / large text", widthDp = 360, heightDp = 800, fontScale = 2f)
@Preview(name = "Exercise / dark tablet", widthDp = 1024, heightDp = 900)
@Composable
private fun ExercisePreview() {
    PulseTheme(true) {
        ExerciseEditorScreen(
            editor = ExerciseEditorState(
                ExerciseDraft(name = "Preview squat", sets = listOf(SetDraft("12", "20"))),
                dirty = true,
            ),
            state = WorkoutUiState(loadState = FitnessLoadState.Ready),
            onAction = {},
            onCancel = {},
        )
    }
}
