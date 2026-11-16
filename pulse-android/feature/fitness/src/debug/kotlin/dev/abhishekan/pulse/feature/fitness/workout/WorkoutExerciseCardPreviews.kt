package dev.abhishekan.pulse.feature.fitness.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.domain.model.WorkoutExercise
import dev.abhishekan.pulse.core.domain.model.WorkoutSet
import dev.abhishekan.pulse.core.ui.PulseSpacing
import dev.abhishekan.pulse.core.ui.PulseTheme
import java.math.BigDecimal

@Preview(name = "Exercise cards / light", widthDp = 412, showBackground = true)
@Preview(name = "Exercise cards / narrow large text", widthDp = 320, fontScale = 2f, showBackground = true)
@Composable
private fun WorkoutExerciseCardLightPreview() {
    WorkoutExerciseCardPreview(dark = false)
}

@Preview(name = "Exercise cards / dark", widthDp = 412, showBackground = true)
@Composable
private fun WorkoutExerciseCardDarkPreview() {
    WorkoutExerciseCardPreview(dark = true)
}

@Composable
private fun WorkoutExerciseCardPreview(dark: Boolean) {
    PulseTheme(dark) {
        Column(
            Modifier.background(PulseTheme.colors.background).padding(PulseSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(PulseSpacing.compact),
        ) {
            WorkoutExerciseCard(
                shoulderPress,
                number = 1,
                expanded = true,
                enabled = true,
                onToggle = {},
                onEdit = {},
                onRemove = {},
            )
            WorkoutExerciseCard(
                plateRaise,
                number = 2,
                expanded = false,
                enabled = true,
                onToggle = {},
                onEdit = {},
                onRemove = {},
            )
        }
    }
}

private val shoulderPress =
    WorkoutExercise(
        id = 1,
        name = "Dumbbell shoulder press",
        catalogId = "shoulder-press",
        catalog = CatalogExercise("shoulder-press", "Dumbbell shoulder press", "dumbbell", listOf("shoulders"), listOf("Press overhead.")),
        sets = listOf(WorkoutSet(1, 12, BigDecimal("7.5")), WorkoutSet(2, 12, BigDecimal.TEN)),
    )

private val plateRaise =
    WorkoutExercise(
        id = 2,
        name = "Front plate raise",
        catalogId = "plate-raise",
        catalog = CatalogExercise("plate-raise", "Front plate raise", "other", listOf("shoulders"), emptyList()),
        sets = (1..3).map { WorkoutSet(it, 10, BigDecimal.TEN) },
    )
