package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.repository.HorizonRepository
class GetHorizonTransactions(private val repository: HorizonRepository) {
    suspend operator fun invoke(page: Int, search: String, type: String?): Outcome<HorizonTransactionPage> =
        if (page < 1 || type !in listOf(null, "debit", "credit")) Outcome.Error(Failure.InvalidInput)
        else repository.transactions(page, search.trim(), type)
}
