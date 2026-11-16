package dev.abhishekan.pulse.feature.garage.shared

import dev.abhishekan.pulse.core.domain.model.Failure

internal sealed interface GarageSection<out T> {
    val value: T?
    val loading: Boolean get() = this is Loading
    val error: Failure? get() = (this as? Failed)?.failure
    val ready: Boolean get() = this is Ready && value != null

    data object Idle : GarageSection<Nothing> {
        override val value: Nothing? = null
    }

    data class Loading<T>(override val value: T? = null) : GarageSection<T>

    data class Ready<T>(override val value: T) : GarageSection<T>

    data class Failed<T>(
        val failure: Failure,
        override val value: T? = null,
    ) : GarageSection<T>
}
