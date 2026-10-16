package dev.abhishekan.pulse.core.domain.model

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Error(val failure: Failure) : Outcome<Nothing>
}
