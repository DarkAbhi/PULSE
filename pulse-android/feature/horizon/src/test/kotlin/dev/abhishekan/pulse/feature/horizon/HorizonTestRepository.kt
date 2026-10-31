package dev.abhishekan.pulse.feature.horizon
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import dev.abhishekan.pulse.core.domain.model.HorizonTransactionPage
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.PlannedPurchases
import dev.abhishekan.pulse.core.domain.repository.HorizonRepository
import kotlinx.coroutines.flow.MutableStateFlow

internal class HorizonTestRepository : HorizonRepository {
    override val changes = MutableStateFlow(0L)
    var snapshot =
        HorizonOverview(
            "₹",
            "100.00".toBigDecimal(),
            "20.00".toBigDecimal(),
            "70.00".toBigDecimal(),
            "0".toBigDecimal(),
            "0".toBigDecimal(),
            "10.00".toBigDecimal(),
            "80".toBigDecimal(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
        )
    var overviewFailure: Failure? = null
    var listFailure: Failure? = null
    var purchaseFailure: Failure? = null
    var writes = mutableListOf<HorizonChange>()
    val pages = mutableListOf<Triple<Int, String, String?>>()
    var readCount = 0
    var write: suspend () -> Outcome<Unit> = { Outcome.Success(Unit) }

    override suspend fun overview(): Outcome<HorizonOverview> {
        readCount++
        return overviewFailure?.let { Outcome.Error(it) }
            ?: Outcome.Success(snapshot)
    }

    override suspend fun transactions(
        page: Int,
        search: String,
        type: String?,
    ): Outcome<HorizonTransactionPage> {
        pages += Triple(page, search, type)
        return listFailure?.let { Outcome.Error(it) }
            ?: Outcome.Success(HorizonTransactionPage(emptyList(), page, 20, 80, 4))
    }

    override suspend fun purchases(): Outcome<PlannedPurchases> =
        purchaseFailure?.let { Outcome.Error(it) }
            ?: Outcome.Success(PlannedPurchases(emptyList(), "0".toBigDecimal()))

    override suspend fun payments(subscriptionId: Long): Outcome<List<HorizonTransaction>> = Outcome.Success(emptyList())

    override suspend fun apply(change: HorizonChange): Outcome<Unit> {
        writes += change
        return write().also { changes.value++ }
    }
}
