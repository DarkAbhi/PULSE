package dev.abhishekan.pulse.feature.fitness.shared

import dev.abhishekan.pulse.core.domain.model.Failure

sealed interface FitnessLoadState {
    data class Loading(val previousFailure: Failure? = null) : FitnessLoadState

    data object Ready : FitnessLoadState

    data class Failed(val failure: Failure) : FitnessLoadState

    val error: Failure?
        get() =
            when (this) {
                is Loading -> previousFailure
                Ready -> null
                is Failed -> failure
            }
}
