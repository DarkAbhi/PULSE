package dev.abhishekan.pulse.core.domain.model

import java.math.BigDecimal

data class PlannedPurchases(
    val items: List<PlannedPurchase>,
    val total: BigDecimal,
)
