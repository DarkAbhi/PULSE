package dev.abhishekan.pulse.core.domain

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Error(val failure: Failure) : Outcome<Nothing>
}

enum class Failure {
    InvalidInput, InvalidCredentials, Unauthorized, Connection, Timeout, Server, Malformed, Storage;
    val message: String get() = when (this) {
        InvalidInput -> "Check your username and password."
        InvalidCredentials -> "That username or password doesn’t match. Please try again."
        Unauthorized -> "Your session has expired. Please sign in again."
        Connection -> "We couldn’t connect. Check your connection and try again."
        Timeout -> "The request took too long. Please try again."
        Server -> "Pulse is temporarily unavailable. Please try again shortly."
        Malformed -> "We couldn’t read this update. Please try again."
        Storage -> "We couldn’t access secure session storage. Please try again."
    }
}
