package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.repository.FitnessRepository

class GetFitnessOverview(
    private val repository: FitnessRepository,
) {
    suspend operator fun invoke() = repository.overview()
}
