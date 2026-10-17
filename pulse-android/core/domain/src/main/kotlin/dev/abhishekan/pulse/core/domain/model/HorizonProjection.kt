package dev.abhishekan.pulse.core.domain.model

import java.math.BigDecimal

data class HorizonProjection(
    val months: Int,
    val label: String,
    val amount: BigDecimal,
)
