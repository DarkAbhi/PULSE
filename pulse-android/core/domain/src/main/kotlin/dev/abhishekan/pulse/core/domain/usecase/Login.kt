package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.AuthRepository

class Login(private val repository: AuthRepository) {
    suspend operator fun invoke(username: String, password: String): Outcome<Unit> {
        val normalized = username.trim()
        if (normalized.isEmpty() || password.isEmpty()) return Outcome.Error(Failure.InvalidInput)
        return repository.login(normalized, password)
    }
}
