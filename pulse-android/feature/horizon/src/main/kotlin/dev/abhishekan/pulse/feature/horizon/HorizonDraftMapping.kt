package dev.abhishekan.pulse.feature.horizon
import dev.abhishekan.pulse.core.domain.model.HorizonSubscription
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import java.time.ZoneId

internal fun HorizonTransaction.draft(currency: String): HorizonDraft {
    val local = date.atZone(ZoneId.systemDefault())
    return HorizonDraft(
        HorizonForm.Transaction,
        id,
        name,
        amount.toPlainString(),
        currency,
        local.toLocalDate().toString(),
        local.toLocalTime().toString(),
        type,
        categoryId = categoryId,
        budgetId = budgetId,
        subscriptionId = subscriptionId,
        notes = notes.orEmpty(),
    )
}

internal fun HorizonSubscription.draft(currency: String) =
    HorizonDraft(
        HorizonForm.Subscription,
        id,
        name,
        amount.toPlainString(),
        currency,
        date = renewalDate?.toString().orEmpty(),
        cycle = cycle,
        dueDay = billingDay?.toString().orEmpty(),
        status = status,
        categoryId = categoryId,
        budgetId = budgetId,
        deductionId = deductionId,
        notes = notes.orEmpty(),
    )
