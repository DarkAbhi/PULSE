package dev.abhishekan.pulse.core.domain.repository

import dev.abhishekan.pulse.core.domain.model.*
import kotlinx.coroutines.flow.StateFlow

interface HorizonRepository {
    val changes: StateFlow<Long>
    suspend fun overview(): Outcome<HorizonOverview>
    suspend fun transactions(page: Int, search: String, type: String?): Outcome<HorizonTransactionPage>
    suspend fun purchases(): Outcome<PlannedPurchases>
    suspend fun payments(subscriptionId: Long): Outcome<List<HorizonTransaction>>
    suspend fun apply(change: HorizonChange): Outcome<Unit>
}
