package dev.abhishekan.pulse.feature.fitness.workout.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.feature.fitness.shared.FitnessLoadState
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutUiState

@Preview(name = "Exercise / narrow phone", widthDp = 320, heightDp = 800)
@Preview(name = "Exercise / large text", widthDp = 320, heightDp = 800, fontScale = 1.6f)
@Preview(name = "Exercise / dark tablet", widthDp = 1024, heightDp = 900)
@Composable
private fun ExercisePreview() {
    PulseTheme(true) {
        ExerciseEditorScreen(
            editor =
                ExerciseEditorState(
                    draft =
                        ExerciseDraft(
                            exerciseId = 1,
                            name = "Dumbbell shoulder press",
                            sets = listOf(SetDraft("12", "7.5"), SetDraft("12", "10"), SetDraft("12", "12.5")),
                            catalogId = "shoulder-press",
                        ),
                    selectedCatalog =
                        CatalogExercise(
                            id = "shoulder-press",
                            name = "Dumbbell Shoulder Press",
                            equipment = "dumbbell",
                            muscles = listOf("shoulders"),
                            instructions = listOf("Press the dumbbells overhead, then lower them with control."),
                        ),
                ),
            state = WorkoutUiState(loadState = FitnessLoadState.Ready),
            onAction = {},
            onCancel = {},
        )
    }
}
