package dev.abhishekan.pulse.core.ui

import dev.abhishekan.pulse.core.domain.model.Failure

val Failure.message: String get() = when (this) {
    Failure.InvalidInput -> "Check your username and password."
    Failure.InvalidCredentials -> "That username or password doesn’t match. Please try again."
    Failure.Unauthorized -> "Your session has expired. Please sign in again."
    Failure.Connection -> "We couldn’t connect. Check your connection and try again."
    Failure.Timeout -> "The request took too long. Please try again."
    Failure.Server -> "Pulse is temporarily unavailable. Please try again shortly."
    Failure.Malformed -> "We couldn’t read this update. Please try again."
    Failure.Storage -> "We couldn’t access secure session storage. Please try again."
}
