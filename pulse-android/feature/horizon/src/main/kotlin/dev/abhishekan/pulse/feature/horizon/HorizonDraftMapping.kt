package dev.abhishekan.pulse.feature.horizon

import dev.abhishekan.pulse.core.domain.model.HorizonSubscription
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import dev.abhishekan.pulse.core.domain.model.UserTimeZone

internal fun HorizonTransaction.draft(currency: String): HorizonDraft {
    val local = date.atZone(UserTimeZone.zone)
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
        originalInstant = date.toString(),
    )
}

internal fun HorizonSubscription.draft(currency: String) =
    HorizonDraft(
        HorizonForm.Subscription,
        id,
        name,
        amount.toPlainString(),
        currency,
        date = (renewalDate ?: nextRenewal ?: java.time.LocalDate.now(UserTimeZone.zone)).toString(),
        cycle = cycle,
        dueDay = (billingDay ?: 1).toString(),
        status = status,
        categoryId = categoryId,
        budgetId = budgetId,
        deductionId = deductionId,
        notes = notes.orEmpty(),
    )
