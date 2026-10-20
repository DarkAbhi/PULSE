package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.repository.ProfileRepository

class GetAccountProfile(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke() = repository.profile()
}
