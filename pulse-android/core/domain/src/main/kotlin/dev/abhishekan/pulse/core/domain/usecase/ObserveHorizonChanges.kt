package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.repository.HorizonRepository

class ObserveHorizonChanges(
    private val repository: HorizonRepository,
) {
    operator fun invoke() = repository.changes
}
