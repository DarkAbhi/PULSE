package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.HorizonRepository
import java.math.BigDecimal

class ApplyHorizonChange(
    private val repository: HorizonRepository,
) {
    suspend operator fun invoke(change: HorizonChange): Outcome<Unit> =
        if (valid(change)) repository.apply(change) else Outcome.Error(Failure.InvalidInput)

    fun valid(change: HorizonChange): Boolean {
        fun text(
            value: String,
            max: Int,
        ) = value.isNotBlank() && value.codePointCount(0, value.length) <= max

        fun money(
            value: BigDecimal,
            positive: Boolean = false,
        ) = value >= BigDecimal.ZERO &&
            (!positive || value > BigDecimal.ZERO) &&
            value <= BigDecimal("9999999999.99") &&
            value.stripTrailingZeros().scale() <= 2

        fun id(value: Long?) = value == null || value > 0

        fun day(value: Int?) = value == null || value in 1..31
        return when (change) {
            is HorizonChange.Config -> money(change.amount) && text(change.currency, 10)
            is HorizonChange.Budget ->
                id(change.id) &&
                    text(change.name, 160) &&
                    money(change.amount)
            is HorizonChange.Deduction ->
                id(change.id) &&
                    text(change.name, 160) &&
                    money(change.amount) &&
                    day(change.dueDay) &&
                    id(change.budgetId) &&
                    change.category in
                    listOf("housing", "investment", "bill", "subscription", "debt", "other")
            is HorizonChange.Transaction ->
                id(change.id) &&
                    text(change.name, 255) &&
                    money(change.amount, true) &&
                    change.type in listOf("debit", "credit") &&
                    id(change.categoryId) &&
                    id(change.budgetId) &&
                    id(change.subscriptionId)
            is HorizonChange.Subscription ->
                id(change.id) &&
                    text(change.name, 255) &&
                    money(change.amount) &&
                    day(change.billingDay) &&
                    change.cycle in listOf("monthly", "yearly") &&
                    change.status in listOf("active", "paused", "cancelled") &&
                    id(change.categoryId) &&
                    id(change.budgetId) &&
                    id(change.deductionId)
            is HorizonChange.Category ->
                text(change.name, 100) &&
                    text(change.icon, 50) &&
                    text(change.color, 30)
            is HorizonChange.Purchase -> text(change.name, 160) && money(change.amount)
            is HorizonChange.Delete -> change.id > 0
            is HorizonChange.LinkPayment -> change.subscriptionId > 0 && change.transactionId > 0
            HorizonChange.ClearPurchases -> true
        }
    }
}
