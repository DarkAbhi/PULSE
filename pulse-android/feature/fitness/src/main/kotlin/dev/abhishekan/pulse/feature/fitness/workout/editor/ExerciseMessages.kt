package dev.abhishekan.pulse.feature.fitness.workout.editor

import dev.abhishekan.pulse.core.domain.model.ExerciseIssue
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.fitness.R

internal fun ExerciseIssue.message(): UiText =
    when (this) {
        ExerciseIssue.Name -> uiText(R.string.pulse_fitness_use_an_exercise_name_with_1_100_characters)
        ExerciseIssue.Sets -> uiText(R.string.pulse_fitness_include_1_20_sets)
        ExerciseIssue.Reps -> uiText(R.string.pulse_fitness_each_set_needs_1_1_000_repetitions)
        ExerciseIssue.Weight ->
            uiText(
                R.string.pulse_fitness_use_a_non_negative_finite_weight_or_leave_it,
            )
        ExerciseIssue.Catalog ->
            uiText(
                R.string.pulse_fitness_choose_a_valid_catalogue_match_or_clear_the_match,
            )
        ExerciseIssue.Alias ->
            uiText(
                R.string.pulse_fitness_choose_a_catalogue_match_before_remembering_this_name,
            )
    }
