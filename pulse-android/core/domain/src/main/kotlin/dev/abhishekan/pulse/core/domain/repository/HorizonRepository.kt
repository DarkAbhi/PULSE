package dev.abhishekan.pulse.core.domain.repository
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import dev.abhishekan.pulse.core.domain.model.HorizonTransactionPage
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.PlannedPurchases
import kotlinx.coroutines.flow.StateFlow

interface HorizonRepository {
    val changes: StateFlow<Long>

    suspend fun overview(): Outcome<HorizonOverview>

    suspend fun transactions(
        page: Int,
        search: String,
        type: String?,
    ): Outcome<HorizonTransactionPage>

    suspend fun purchases(): Outcome<PlannedPurchases>

    suspend fun payments(subscriptionId: Long): Outcome<List<HorizonTransaction>>

    suspend fun apply(change: HorizonChange): Outcome<Unit>
}
