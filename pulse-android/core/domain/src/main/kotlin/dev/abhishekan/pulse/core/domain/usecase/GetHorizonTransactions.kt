package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.HorizonTransactionPage
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.HorizonRepository

class GetHorizonTransactions(
    private val repository: HorizonRepository,
) {
    suspend operator fun invoke(
        page: Int,
        search: String,
        type: String?,
        categoryId: Long? = null,
        pageSize: Int = 20,
    ): Outcome<HorizonTransactionPage> =
        if (page < 1 ||
            pageSize !in 1..100 ||
            (categoryId != null && categoryId <= 0) ||
            type !in listOf(null, "debit", "credit")
        ) {
            Outcome.Error(Failure.InvalidInput)
        } else {
            repository.transactions(page, search.trim(), type, categoryId, pageSize)
        }
}
