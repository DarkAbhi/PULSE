package dev.abhishekan.pulse.core.domain.model

import java.math.BigDecimal

data class HorizonOverview(val currency: String, val baseAmount: BigDecimal, val remaining: BigDecimal, val deductionsTotal: BigDecimal,
    val transactionsTotal: BigDecimal, val allocatedTotal: BigDecimal, val subscriptionBurn: BigDecimal, val committedPercent: BigDecimal,
    val budgets: List<HorizonBudget>, val deductions: List<HorizonDeduction>, val categories: List<HorizonCategory>,
    val subscriptions: List<HorizonSubscription>, val projections: List<HorizonProjection>) {
    val isOvercommitted: Boolean get() = remaining.signum() < 0
}
