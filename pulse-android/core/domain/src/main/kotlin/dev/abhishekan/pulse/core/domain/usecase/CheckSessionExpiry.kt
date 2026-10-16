package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.repository.AuthRepository

class CheckSessionExpiry(private val repository: AuthRepository) { suspend operator fun invoke() = repository.checkExpiry() }
