package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.repository.HorizonRepository

class LoadHorizon(
    private val repository: HorizonRepository,
) {
    suspend operator fun invoke() = repository.overview()
}
