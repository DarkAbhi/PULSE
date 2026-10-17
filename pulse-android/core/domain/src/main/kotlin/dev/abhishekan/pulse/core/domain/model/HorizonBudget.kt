package dev.abhishekan.pulse.core.domain.model

import java.math.BigDecimal

data class HorizonBudget(val id: Long, val name: String, val allocated: BigDecimal, val used: BigDecimal, val available: BigDecimal, val usagePercent: BigDecimal) {
    val isOverBudget: Boolean get() = available.signum() < 0
}
