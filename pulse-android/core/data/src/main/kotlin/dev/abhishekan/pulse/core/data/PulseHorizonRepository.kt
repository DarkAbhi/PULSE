package dev.abhishekan.pulse.core.data

import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.repository.HorizonRepository
import dev.abhishekan.pulse.core.network.HorizonApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import java.math.BigDecimal

class PulseHorizonRepository(private val api: HorizonApi, private val auth: PulseAuthRepository) : HorizonRepository {
    private val revision = MutableStateFlow(0L)
    override val changes = revision.asStateFlow()
    private suspend fun <T> request(mutation: Boolean = false, block: suspend () -> T): Outcome<T> {
        val version = auth.version
        return try {
            val value = withContext(Dispatchers.Default) { block() }
            if (mutation) revision.update { it + 1 }
            Outcome.Success(value)
        } catch (error: Exception) {
            val failure = remoteFailure(error)
            if (failure == Failure.Unauthorized) auth.expire(version)
            // A committed write may lose its response; observers must refresh before another attempt.
            if (mutation) revision.update { it + 1 }
            Outcome.Error(failure)
        }
    }
    override suspend fun overview() = request { api.overview().domain() }
    override suspend fun transactions(page: Int, search: String, type: String?) = request {
        val dto = api.transactions(page, search, type)
        require(dto.page > 0 && dto.pageSize in 1..100 && dto.total >= 0 && dto.totalPages >= 0)
        val items = dto.transactions.orEmpty().map { it.domain() }
        require(items.size <= dto.pageSize)
        HorizonTransactionPage(items, dto.page, dto.pageSize, dto.total, dto.totalPages)
    }
    override suspend fun purchases() = request {
        val dto = api.purchases()
        PlannedPurchases(dto.items.orEmpty().map { require(it.id > 0 && it.name.isNotBlank() && it.price.horizonDecimal().signum() >= 0); PlannedPurchase(it.id, it.name, it.price.horizonDecimal(), it.url) }, dto.total.horizonDecimal())
    }
    override suspend fun payments(subscriptionId: Long) = request { api.payments(subscriptionId).map { it.domain() } }
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun apply(change: HorizonChange): Outcome<Unit> = request(true) {
        fun BigDecimal.numeric() = JsonUnquotedLiteral(toPlainString())
        fun JsonObjectBuilder.ref(key: String, id: Long?) { put(key, id?.let(::JsonPrimitive) ?: JsonNull) }
        fun JsonObjectBuilder.text(key: String, value: String?) { put(key, value?.let(::JsonPrimitive) ?: JsonNull) }
        when (change) {
            is HorizonChange.Config -> api.config(buildJsonObject { put("base_amount", change.amount.numeric()); put("currency", change.currency.trim()) })
            is HorizonChange.Budget -> api.save("horizon/budgets", change.id, buildJsonObject { put("name", change.name.trim()); put("allocated_amount", change.amount.numeric()) })
            is HorizonChange.Deduction -> api.save("horizon/deductions", change.id, buildJsonObject {
                put("name", change.name.trim()); put("amount", change.amount.numeric()); put("category", change.category)
                put("due_day", change.dueDay?.let(::JsonPrimitive) ?: JsonNull); put("is_active", change.active); ref("budget_id", change.budgetId)
            })
            is HorizonChange.Transaction -> api.save("horizon/transactions", change.id, buildJsonObject {
                put("name", change.name.trim()); put("amount", change.amount.numeric()); put("type", change.type); put("transaction_date", change.date.toString())
                ref("category_id", change.categoryId); ref("budget_id", change.budgetId); ref("subscription_id", change.subscriptionId); text("notes", change.notes)
            })
            is HorizonChange.Subscription -> api.save("horizon/subscriptions", change.id, buildJsonObject {
                put("name", change.name.trim()); put("amount", change.amount.numeric()); put("billing_cycle", change.cycle); put("status", change.status)
                put("billing_day", change.billingDay?.let(::JsonPrimitive) ?: JsonNull); text("renewal_date", change.renewalDate?.toString())
                ref("category_id", change.categoryId); ref("budget_id", change.budgetId); ref("deduction_id", change.deductionId); text("notes", change.notes)
            })
            is HorizonChange.Category -> api.save("horizon/categories", null, buildJsonObject { put("name", change.name.trim()); put("icon", change.icon.trim()); put("color", change.color.trim()) })
            is HorizonChange.Purchase -> api.save("next-month-purchases", null, buildJsonObject { put("name", change.name.trim()); put("price", change.amount.numeric()); text("url", change.url) })
            is HorizonChange.LinkPayment -> api.save("horizon/subscriptions/${change.subscriptionId}/transactions", null, buildJsonObject { put("transaction_id", change.transactionId) })
            is HorizonChange.Delete -> api.delete(when (change.kind) {
                HorizonRecord.Budget -> "horizon/budgets"; HorizonRecord.Deduction -> "horizon/deductions"; HorizonRecord.Transaction -> "horizon/transactions"
                HorizonRecord.Subscription -> "horizon/subscriptions"; HorizonRecord.Purchase -> "next-month-purchases"
            } + "/${change.id}")
            HorizonChange.ClearPurchases -> api.delete("next-month-purchases")
        }
    }
}
