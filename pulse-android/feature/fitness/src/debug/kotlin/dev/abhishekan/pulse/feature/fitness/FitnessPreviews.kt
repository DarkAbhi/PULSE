package dev.abhishekan.pulse.feature.fitness
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.FitnessMetric
import dev.abhishekan.pulse.core.domain.model.FitnessOverview
import dev.abhishekan.pulse.core.domain.model.WorkoutSummary
import dev.abhishekan.pulse.core.ui.PulseTheme
import java.time.LocalDate
import java.time.YearMonth

private val day = LocalDate.of(2026, 10, 17)
private val sample =
    FitnessUiState(
        day,
        YearMonth.from(day),
        FitnessOverview(
            day,
            listOf(
                FitnessMetric("last_workout", "Last worked out", "Today", "at gym", null),
                FitnessMetric("attendance", "Gym attendance", "8.28 %", "of days so far this year", null),
                FitnessMetric("workouts", "Total workouts", "24", "this year", null),
                FitnessMetric("streak", "Workout streak", "3 weeks", "with a gym visit", null),
                FitnessMetric("trained", "Total trained", "8h 43m", "This year", "2 days missing timing"),
                FitnessMetric("volume", "Volume lifted", "1,615 kg", "recorded sets this year", null),
                FitnessMetric("sets", "Sets", "16", "recorded this year", null),
                FitnessMetric("preference", "Preferred workout time", "Afternoon", "from recorded starts this year", null),
            ),
            listOf(WorkoutSummary(1, day, "7:30 AM")),
        ),
        loading = false,
    )

@Preview(name = "Fitness / phone light", widthDp = 360, heightDp = 800)
@Preview(name = "Fitness / tablet light", widthDp = 1024, heightDp = 900)
@Preview(name = "Fitness / landscape", widthDp = 800, heightDp = 360)
@Preview(name = "Fitness / large text", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun FitnessLightPreview() {
    PulseTheme(false) { FitnessScreen(sample, {}, {}, {}, {}, {}, {}, {}) }
}

@Preview(name = "Fitness / tablet dark", widthDp = 1024, heightDp = 900)
@Composable
private fun FitnessDarkPreview() {
    PulseTheme(true) { FitnessScreen(sample, {}, {}, {}, {}, {}, {}, {}) }
}

@Preview(name = "Fitness / error", widthDp = 360, heightDp = 800)
@Composable
private fun FitnessErrorPreview() {
    PulseTheme(false) {
        FitnessScreen(sample.copy(overview = null, error = Failure.Connection), {
        }, {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Exercise / large text", widthDp = 360, heightDp = 800, fontScale = 2f)
@Preview(name = "Exercise / dark tablet", widthDp = 1024, heightDp = 900)
@Composable
private fun ExercisePreview() {
    PulseTheme(true) {
        WorkoutScreen(
            WorkoutUiState(
                loading = false,
                editor =
                    ExerciseEditorState(
                        ExerciseDraft(name = "Preview squat", sets = listOf(SetDraft("12", "20"))),
                        dirty = true,
                    ),
            ),
            {},
            {},
            {},
            {},
            {},
            {},
            { _, _, _ -> },
            {},
            {},
            {},
            {},
            {},
            {},
            {},
            {},
        )
    }
}
