package dev.abhishekan.pulse.core.domain.repository

import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.SessionState
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val session: StateFlow<SessionState>
    suspend fun restore()
    suspend fun login(username: String, password: String): Outcome<Unit>
    suspend fun logout(): Outcome<Unit>
    suspend fun forget(): Outcome<Unit>
    suspend fun checkExpiry()
}
