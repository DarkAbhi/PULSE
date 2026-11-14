package dev.abhishekan.pulse.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable data class HorizonOverviewDto(
    val currency: String,
    @SerialName("base_amount") val base: JsonPrimitive,
    @SerialName("remaining_amount") val remaining: JsonPrimitive,
    @SerialName("total_deductions") val deductionsTotal: JsonPrimitive,
    @SerialName("total_transactions") val transactionsTotal: JsonPrimitive,
    @SerialName("total_budgets_allocated") val allocatedTotal: JsonPrimitive,
    @SerialName("total_subscription_burn") val subscriptionBurn: JsonPrimitive,
    @SerialName("committed_ratio") val committedPercent: JsonPrimitive,
    val budgets: List<HorizonBudgetDto>? = null,
    val deductions: List<HorizonDeductionDto>? = null,
    val categories: List<HorizonCategoryDto>? = null,
    val subscriptions: List<HorizonSubscriptionDto>? = null,
    val projections: List<HorizonProjectionDto>? = null,
)

@Serializable data class HorizonBudgetDto(
    val id: Long,
    val name: String,
    @SerialName("allocated_amount") val allocated: JsonPrimitive,
    @SerialName("used_amount") val used: JsonPrimitive,
    @SerialName("available_amount") val available: JsonPrimitive,
    @SerialName("usage_percentage") val usagePercent: JsonPrimitive,
)

@Serializable data class HorizonDeductionDto(
    val id: Long,
    val name: String,
    val category: String,
    val amount: JsonPrimitive,
    @SerialName("due_day") val dueDay: Int? = null,
    @SerialName("is_active") val active: Boolean,
    @SerialName("budget_id") val budgetId: Long? = null,
)

@Serializable data class HorizonCategoryDto(
    val id: Long,
    val name: String,
    val icon: String = "tag",
    val color: String = "#64748b",
    @SerialName("is_default") val isDefault: Boolean = false,
)

@Serializable data class HorizonSubscriptionDto(
    val id: Long,
    val name: String,
    val amount: JsonPrimitive,
    @SerialName("billing_cycle") val cycle: String,
    @SerialName("billing_day") val billingDay: Int? = null,
    @SerialName("renewal_date") val renewal: String? = null,
    @SerialName("next_renewal_date") val nextRenewal: String? = null,
    @SerialName("monthly_equivalent_amount") val monthly: JsonPrimitive,
    val status: String,
    @SerialName("category_id") val categoryId: Long? = null,
    @SerialName("budget_id") val budgetId: Long? = null,
    @SerialName("deduction_id") val deductionId: Long? = null,
    val notes: String? = null,
    @SerialName("linked_transaction_count") val linkedCount: Int = 0,
    @SerialName("total_spent") val spent: JsonPrimitive,
)

@Serializable data class HorizonTransactionDto(
    val id: Long,
    val name: String,
    val amount: JsonPrimitive,
    val type: String = "debit",
    @SerialName("transaction_date") val date: String,
    @SerialName("category_id") val categoryId: Long? = null,
    @SerialName("category_name") val categoryName: String = "Other",
    @SerialName("budget_id") val budgetId: Long? = null,
    @SerialName("budget_name") val budgetName: String? = null,
    @SerialName("subscription_id") val subscriptionId: Long? = null,
    @SerialName("subscription_name") val subscriptionName: String? = null,
    val notes: String? = null,
)

@Serializable data class HorizonTransactionPageDto(
    val transactions: List<HorizonTransactionDto>? = null,
    val page: Int,
    @SerialName("page_size") val pageSize: Int,
    val total: Int,
    @SerialName("total_pages") val totalPages: Int,
)

@Serializable data class HorizonProjectionDto(
    val months: Int,
    val label: String,
    @SerialName("cumulative_uncommitted") val amount: JsonPrimitive,
)

@Serializable data class PlannedPurchaseDto(
    val id: Long,
    val name: String,
    val price: JsonPrimitive,
    val url: String? = null,
)

@Serializable data class PlannedPurchasesDto(
    val items: List<PlannedPurchaseDto>? = null,
    val total: JsonPrimitive,
)
