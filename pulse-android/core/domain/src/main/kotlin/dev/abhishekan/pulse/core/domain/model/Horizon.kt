package dev.abhishekan.pulse.core.domain.model

import java.math.BigDecimal

data class Horizon(
    val currency: String,
    val baseAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val deductions: BigDecimal,
) {
    val isOvercommitted: Boolean get() = remainingAmount.signum() < 0
}
