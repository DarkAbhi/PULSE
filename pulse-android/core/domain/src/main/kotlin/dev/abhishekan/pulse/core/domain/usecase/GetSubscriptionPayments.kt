package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.repository.HorizonRepository
class GetSubscriptionPayments(private val repository: HorizonRepository) {
    suspend operator fun invoke(id: Long): Outcome<List<HorizonTransaction>> =
        if (id <= 0) Outcome.Error(Failure.InvalidInput) else repository.payments(id)
}
