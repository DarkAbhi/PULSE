package dev.abhishekan.pulse.feature.fitness.shared

import dev.abhishekan.pulse.core.domain.model.Failure

enum class FitnessReviewState {
    None,
    AwaitingRefresh,
    Ready;

    internal fun afterRefresh(succeeded: Boolean): FitnessReviewState =
        when {
            this == None -> None
            succeeded -> Ready
            else -> AwaitingRefresh
        }
}

internal val Failure.requiresFitnessReview: Boolean
    get() = this != Failure.InvalidInput && this != Failure.Unauthorized && this != Failure.NotFound
