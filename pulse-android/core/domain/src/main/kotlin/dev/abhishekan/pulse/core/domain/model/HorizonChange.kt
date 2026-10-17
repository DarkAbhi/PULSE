package dev.abhishekan.pulse.core.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

sealed interface HorizonChange {
    data class Config(val amount: BigDecimal, val currency: String) : HorizonChange
    data class Budget(val id: Long?, val name: String, val amount: BigDecimal) : HorizonChange
    data class Deduction(val id: Long?, val name: String, val amount: BigDecimal, val category: String, val dueDay: Int?, val active: Boolean, val budgetId: Long?) : HorizonChange
    data class Transaction(val id: Long?, val name: String, val amount: BigDecimal, val type: String, val date: Instant,
        val categoryId: Long?, val budgetId: Long?, val subscriptionId: Long?, val notes: String?) : HorizonChange
    data class Subscription(val id: Long?, val name: String, val amount: BigDecimal, val cycle: String, val billingDay: Int?, val renewalDate: LocalDate?,
        val status: String, val categoryId: Long?, val budgetId: Long?, val deductionId: Long?, val notes: String?) : HorizonChange
    data class Category(val name: String, val icon: String, val color: String) : HorizonChange
    data class Purchase(val name: String, val amount: BigDecimal, val url: String?) : HorizonChange
    data class Delete(val kind: HorizonRecord, val id: Long) : HorizonChange
    data class LinkPayment(val subscriptionId: Long, val transactionId: Long) : HorizonChange
    data object ClearPurchases : HorizonChange
}
