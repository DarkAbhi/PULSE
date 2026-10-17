package dev.abhishekan.pulse.feature.horizon
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import dev.abhishekan.pulse.core.domain.model.HorizonTransactionPage
import dev.abhishekan.pulse.core.domain.model.PlannedPurchases
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.feature.horizon.R

enum class HorizonTab(
    @param:androidx.annotation.StringRes val label: Int,
) {
    Overview(
        R.string.pulse_horizon_overview,
    ),
    Transactions(
        R.string.pulse_horizon_transactions,
    ),
    Budgets(
        R.string.pulse_horizon_budgets,
    ),
    Obligations(
        R.string.pulse_horizon_obligations,
    ),
    Subscriptions(
        R.string.pulse_horizon_subscriptions,
    ),
    Purchases(R.string.pulse_horizon_planned_purchases),
}

data class HorizonSection<T>(
    val value: T? = null,
    val loading: Boolean = true,
    val error: Failure? = null,
)

data class HorizonUiState(
    val tab: HorizonTab = HorizonTab.Overview,
    val overview: HorizonSection<HorizonOverview> = HorizonSection(),
    val transactions: HorizonSection<HorizonTransactionPage> = HorizonSection(),
    val purchases: HorizonSection<PlannedPurchases> = HorizonSection(),
    val payments: HorizonSection<List<HorizonTransaction>> = HorizonSection(loading = false),
    val subscriptionId: Long? = null,
    val search: String = "",
    val type: String? = null,
    val draft: HorizonDraft? = null,
    val busy: Boolean = false,
    val reviewRequired: Boolean = false,
    val message: UiText? = null,
    val formError: UiText? = null,
) {
    val reviewReady: Boolean get() =
        listOf(overview, transactions, purchases).all {
            !it.loading &&
                it.error == null &&
                it.value != null
        } &&
            (
                subscriptionId == null ||
                    (!payments.loading && payments.error == null && payments.value != null)
            )
}
