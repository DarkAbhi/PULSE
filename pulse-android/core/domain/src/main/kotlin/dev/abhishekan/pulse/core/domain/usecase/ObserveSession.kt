package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.repository.AuthRepository

class ObserveSession(private val repository: AuthRepository) { operator fun invoke() = repository.session }
