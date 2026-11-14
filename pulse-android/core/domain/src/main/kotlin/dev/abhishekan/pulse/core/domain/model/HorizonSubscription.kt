package dev.abhishekan.pulse.core.domain.model

import java.math.BigDecimal
import java.time.LocalDate

data class HorizonSubscription(
    val id: Long,
    val name: String,
    val amount: BigDecimal,
    val cycle: String,
    val billingDay: Int?,
    val renewalDate: LocalDate?,
    val nextRenewal: LocalDate?,
    val monthlyAmount: BigDecimal,
    val status: String,
    val categoryId: Long?,
    val budgetId: Long?,
    val deductionId: Long?,
    val notes: String?,
    val linkedCount: Int,
    val totalSpent: BigDecimal,
)
