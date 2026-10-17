package dev.abhishekan.pulse.core.data
import dev.abhishekan.pulse.core.domain.model.HorizonBudget
import dev.abhishekan.pulse.core.domain.model.HorizonCategory
import dev.abhishekan.pulse.core.domain.model.HorizonDeduction
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.HorizonProjection
import dev.abhishekan.pulse.core.domain.model.HorizonSubscription
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import dev.abhishekan.pulse.core.network.dto.HorizonOverviewDto
import dev.abhishekan.pulse.core.network.dto.HorizonTransactionDto
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.LocalDate

internal fun JsonPrimitive.horizonDecimal() = content.toBigDecimal().also { require(!isString) }

private fun checked(
    id: Long,
    name: String,
) {
    require(id > 0 && name.isNotBlank())
}

internal fun HorizonTransactionDto.domain(): HorizonTransaction {
    checked(id, name)
    require(
        type in listOf("debit", "credit") && amount.horizonDecimal().signum() > 0,
    )
    return HorizonTransaction(
        id,
        name,
        amount.horizonDecimal(),
        type,
        Instant.parse(
            date,
        ),
        categoryId,
        categoryName,
        budgetId,
        budgetName,
        subscriptionId,
        subscriptionName,
        notes,
    )
}

internal fun HorizonOverviewDto.domain(): HorizonOverview {
    require(currency.isNotBlank() && base.horizonDecimal().signum() >= 0)
    return HorizonOverview(
        currency,
        base.horizonDecimal(),
        remaining.horizonDecimal(),
        deductionsTotal.horizonDecimal(),
        transactionsTotal.horizonDecimal(),
        allocatedTotal.horizonDecimal(),
        subscriptionBurn.horizonDecimal(),
        committedPercent.horizonDecimal(),
        budgets.orEmpty().map {
            checked(it.id, it.name)
            require(
                it.allocated.horizonDecimal().signum() >= 0,
            )
            HorizonBudget(
                it.id,
                it.name,
                it.allocated.horizonDecimal(),
                it.used.horizonDecimal(),
                it.available.horizonDecimal(),
                it.usagePercent.horizonDecimal(),
            )
        },
        deductions.orEmpty().map {
            checked(it.id, it.name)
            require(
                it.amount.horizonDecimal().signum() >= 0 &&
                    (it.dueDay == null || it.dueDay in 1..31),
            )
            HorizonDeduction(
                it.id,
                it.name,
                it.category,
                it.amount.horizonDecimal(),
                it.dueDay,
                it.active,
                it.budgetId,
            )
        },
        categories.orEmpty().map {
            checked(it.id, it.name)
            HorizonCategory(it.id, it.name, it.icon, it.color, it.isDefault)
        },
        subscriptions.orEmpty().map {
            checked(it.id, it.name)
            require(
                it.amount.horizonDecimal().signum() >= 0 &&
                    it.cycle in listOf("monthly", "yearly") &&
                    it.status in listOf("active", "paused", "cancelled"),
            )
            HorizonSubscription(
                it.id,
                it.name,
                it.amount.horizonDecimal(),
                it.cycle,
                it.billingDay,
                it.renewal?.let { date ->
                    LocalDate.parse(date.take(10))
                },
                it.nextRenewal?.let { date ->
                    LocalDate.parse(date.take(10))
                },
                it.monthly.horizonDecimal(),
                it.status,
                it.categoryId,
                it.budgetId,
                it.deductionId,
                it.notes,
                it.linkedCount,
                it.spent.horizonDecimal(),
            )
        },
        projections.orEmpty().map {
            require(it.months > 0)
            HorizonProjection(it.months, it.label, it.amount.horizonDecimal())
        },
    )
}
