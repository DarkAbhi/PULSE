package dev.abhishekan.pulse.feature.fitness

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.ExerciseIssue
import dev.abhishekan.pulse.core.ui.message

internal fun fitnessFailureMessage(failure: Failure): String = if (failure == Failure.InvalidInput)
    "These values weren’t accepted. Check the name, repetitions, weight and catalogue selection." else failure.message
internal fun ExerciseIssue.message(): String = when (this) {
    ExerciseIssue.Name -> "Use an exercise name with 1–100 characters."
    ExerciseIssue.Sets -> "Include 1–20 sets."
    ExerciseIssue.Reps -> "Each set needs 1–1,000 repetitions."
    ExerciseIssue.Weight -> "Use a non-negative, finite weight, or leave it blank for bodyweight."
    ExerciseIssue.Catalog -> "Choose a valid catalogue match or clear the match."
    ExerciseIssue.Alias -> "Choose a catalogue match before remembering this name."
}
