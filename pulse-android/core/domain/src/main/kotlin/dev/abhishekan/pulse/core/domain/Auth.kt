package dev.abhishekan.pulse.core.domain

import kotlinx.coroutines.flow.StateFlow

sealed interface SessionState {
    data object Checking : SessionState
    data class SignedOut(val expired: Boolean = false, val storageProblem: Boolean = false) : SessionState
    data class Authenticated(val username: String, val expiresAtMillis: Long) : SessionState
    data class Unavailable(val failure: Failure) : SessionState
}

interface AuthRepository {
    val session: StateFlow<SessionState>
    suspend fun restore()
    suspend fun login(username: String, password: String): Outcome<Unit>
    suspend fun logout(): Outcome<Unit>
    suspend fun forget(): Outcome<Unit>
    suspend fun checkExpiry()
}

class ObserveSession(private val repository: AuthRepository) { operator fun invoke() = repository.session }
class RestoreSession(private val repository: AuthRepository) { suspend operator fun invoke() = repository.restore() }
class Login(private val repository: AuthRepository) {
    suspend operator fun invoke(username: String, password: String): Outcome<Unit> {
        val normalized = username.trim()
        if (normalized.isEmpty() || password.isEmpty()) return Outcome.Error(Failure.InvalidInput)
        return repository.login(normalized, password)
    }
}
class Logout(private val repository: AuthRepository) { suspend operator fun invoke() = repository.logout() }
class ForgetSession(private val repository: AuthRepository) { suspend operator fun invoke() = repository.forget() }
class CheckSessionExpiry(private val repository: AuthRepository) { suspend operator fun invoke() = repository.checkExpiry() }
