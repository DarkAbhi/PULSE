package dev.abhishekan.pulse.feature.horizon
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.feature.horizon.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class HorizonForm(
    @param:androidx.annotation.StringRes val label: Int,
) {
    Config(
        R.string.pulse_horizon_starting_pool,
    ),
    Budget(
        R.string.pulse_horizon_budget,
    ),
    Deduction(
        R.string.pulse_horizon_fixed_obligation,
    ),
    Transaction(
        R.string.pulse_horizon_transaction,
    ),
    Subscription(
        R.string.pulse_horizon_subscription,
    ),
    Category(R.string.pulse_horizon_category),
    Purchase(R.string.pulse_horizon_planned_purchase),
}

data class HorizonDraft(
    val kind: HorizonForm,
    val id: Long? = null,
    val name: String = "",
    val amount: String = "",
    val currency: String = "₹",
    val date: String = "",
    val time: String = "00:00",
    val type: String = "debit",
    val category: String = "bill",
    val dueDay: String = "",
    val active: Boolean = true,
    val cycle: String = "monthly",
    val status: String = "active",
    val categoryId: Long? = null,
    val budgetId: Long? = null,
    val subscriptionId: Long? = null,
    val deductionId: Long? = null,
    val notes: String = "",
    val url: String = "",
    val icon: String = "tag",
    val color: String = "#64748b",
    val dirty: Boolean = false,
) {
    fun change(): HorizonChange {
        val value by lazy { amount.toBigDecimal() }

        fun optionalDay() = dueDay.takeIf { it.isNotBlank() }?.toInt()
        return when (kind) {
            HorizonForm.Config -> HorizonChange.Config(value, currency.trim())
            HorizonForm.Budget -> HorizonChange.Budget(id, name.trim(), value)
            HorizonForm.Deduction ->
                HorizonChange.Deduction(
                    id,
                    name.trim(),
                    value,
                    category,
                    optionalDay(),
                    active,
                    budgetId,
                )
            HorizonForm.Transaction ->
                HorizonChange.Transaction(
                    id,
                    name.trim(),
                    value,
                    type,
                    LocalDate
                        .parse(
                            date,
                        ).atTime(
                            LocalTime.parse(time),
                        ).atZone(ZoneId.systemDefault())
                        .toInstant(),
                    categoryId,
                    budgetId,
                    subscriptionId,
                    notes.trim().ifBlank {
                        null
                    },
                )
            HorizonForm.Subscription ->
                HorizonChange.Subscription(
                    id,
                    name.trim(),
                    value,
                    cycle,
                    optionalDay(),
                    date
                        .takeIf {
                            it.isNotBlank()
                        }?.let(LocalDate::parse),
                    status,
                    categoryId,
                    budgetId,
                    deductionId,
                    notes.trim().ifBlank {
                        null
                    },
                )
            HorizonForm.Category -> HorizonChange.Category(name.trim(), icon.trim(), color.trim())
            HorizonForm.Purchase ->
                HorizonChange.Purchase(
                    name.trim(),
                    value,
                    url.trim().ifBlank {
                        null
                    },
                )
        }
    }

    fun saved() =
        arrayListOf(
            kind.name,
            id?.toString().orEmpty(),
            name,
            amount,
            currency,
            date,
            time,
            type,
            category,
            dueDay,
            active.toString(),
            cycle,
            status,
            categoryId?.toString().orEmpty(),
            budgetId?.toString().orEmpty(),
            subscriptionId?.toString().orEmpty(),
            deductionId?.toString().orEmpty(),
            notes,
            url,
            icon,
            color,
            dirty.toString(),
        )

    companion object {
        fun restore(v: ArrayList<String>) =
            HorizonDraft(
                HorizonForm.valueOf(
                    v[0],
                ),
                v[1].toLongOrNull(),
                v[2],
                v[3],
                v[4],
                v[5],
                v[6],
                v[7],
                v[8],
                v[9],
                v[10].toBoolean(),
                v[11],
                v[12],
                v[13].toLongOrNull(),
                v[14].toLongOrNull(),
                v[15].toLongOrNull(),
                v[16].toLongOrNull(),
                v[17],
                v[18],
                v[19],
                v[20],
                v[21].toBoolean(),
            )
    }
}
