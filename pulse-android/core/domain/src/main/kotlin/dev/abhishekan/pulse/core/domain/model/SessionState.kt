package dev.abhishekan.pulse.core.domain.model

sealed interface SessionState {
    data object Checking : SessionState

    data class SignedOut(
        val expired: Boolean = false,
        val storageProblem: Boolean = false,
    ) : SessionState

    data class Authenticated(
        val username: String,
        val expiresAtMillis: Long,
    ) : SessionState

    data class Unavailable(
        val failure: Failure,
    ) : SessionState
}
