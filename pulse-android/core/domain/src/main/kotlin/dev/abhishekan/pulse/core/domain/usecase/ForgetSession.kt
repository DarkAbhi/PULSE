package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.repository.AuthRepository

class ForgetSession(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke() = repository.forget()
}
