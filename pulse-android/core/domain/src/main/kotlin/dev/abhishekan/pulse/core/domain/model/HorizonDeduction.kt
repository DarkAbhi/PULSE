package dev.abhishekan.pulse.core.domain.model

import java.math.BigDecimal

data class HorizonDeduction(val id: Long, val name: String, val category: String, val amount: BigDecimal, val dueDay: Int?, val active: Boolean, val budgetId: Long?)
