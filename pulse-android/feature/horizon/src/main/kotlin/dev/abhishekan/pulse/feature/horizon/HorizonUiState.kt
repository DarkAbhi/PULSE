package dev.abhishekan.pulse.feature.horizon

import dev.abhishekan.pulse.core.domain.model.*

enum class HorizonTab(val label: String) { Overview("Overview"), Transactions("Transactions"), Budgets("Budgets"), Obligations("Obligations"), Subscriptions("Subscriptions"), Purchases("Planned purchases") }
data class HorizonSection<T>(val value: T? = null, val loading: Boolean = true, val error: Failure? = null)
data class HorizonUiState(
    val tab: HorizonTab = HorizonTab.Overview,
    val overview: HorizonSection<HorizonOverview> = HorizonSection(),
    val transactions: HorizonSection<HorizonTransactionPage> = HorizonSection(),
    val purchases: HorizonSection<PlannedPurchases> = HorizonSection(),
    val payments: HorizonSection<List<HorizonTransaction>> = HorizonSection(loading = false),
    val subscriptionId: Long? = null, val search: String = "", val type: String? = null,
    val draft: HorizonDraft? = null, val busy: Boolean = false, val reviewRequired: Boolean = false,
    val message: String? = null, val formError: String? = null,
) {
    val reviewReady: Boolean get() = listOf(overview, transactions, purchases).all { !it.loading && it.error == null && it.value != null } &&
        (subscriptionId == null || (!payments.loading && payments.error == null && payments.value != null))
}
