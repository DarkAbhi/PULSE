package dev.abhishekan.pulse.feature.fitness.shared

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.fitness.R

internal fun fitnessFailureMessage(failure: Failure): UiText =
    if (failure == Failure.InvalidInput) {
        uiText(R.string.pulse_fitness_these_values_weren_t_accepted_check_the_name_repetitions)
    } else {
        failure.message
    }
