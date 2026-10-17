package dev.abhishekan.pulse.core.domain.model

import java.math.BigDecimal
import java.time.Instant

data class HorizonTransaction(
    val id: Long,
    val name: String,
    val amount: BigDecimal,
    val type: String,
    val date: Instant,
    val categoryId: Long?,
    val categoryName: String,
    val budgetId: Long?,
    val budgetName: String?,
    val subscriptionId: Long?,
    val subscriptionName: String?,
    val notes: String?,
)
