package dev.abhishekan.pulse.core.domain.model

import java.math.BigDecimal

data class PlannedPurchase(
    val id: Long,
    val name: String,
    val price: BigDecimal,
    val url: String?,
)
