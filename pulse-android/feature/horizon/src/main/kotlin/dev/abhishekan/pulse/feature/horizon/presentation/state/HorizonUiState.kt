package dev.abhishekan.pulse.feature.horizon.presentation.state

import androidx.annotation.StringRes
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import dev.abhishekan.pulse.core.domain.model.HorizonTransactionPage
import dev.abhishekan.pulse.core.domain.model.PlannedPurchases
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.accounts.AccountType
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonDraft

enum class HorizonTab(
    @param:StringRes val label: Int,
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
    Accounts(R.string.pulse_horizon_accounts),
}

data class HorizonSection<T>(
    val value: T? = null,
    val loading: Boolean = true,
    val error: Failure? = null,
)

internal data class HorizonUiState(
    val tab: HorizonTab = HorizonTab.Overview,
    val overview: HorizonSection<HorizonOverview> = HorizonSection(),
    val transactionLists: TransactionListsState = TransactionListsState(),
    val importOpen: Boolean = false,
    val accountList: AccountType? = null,
    val transactions: HorizonSection<HorizonTransactionPage> = HorizonSection(),
    val purchases: HorizonSection<PlannedPurchases> = HorizonSection(),
    val payments: HorizonSection<List<HorizonTransaction>> = HorizonSection(loading = false),
    val subscriptionId: Long? = null,
    val search: String = "",
    val type: String? = null,
    val draft: HorizonDraft? = null,
    val transactionDraft: HorizonDraft? = null,
    val busy: Boolean = false,
    val reviewRequired: Boolean = false,
    val message: UiText? = null,
    val formError: UiText? = null,
    val showingReviewRecords: Boolean = false,
    val confirmation: HorizonConfirmation? = null,
) {
    val reviewReady: Boolean get() =
        listOf(overview, transactionLists.recent, purchases).all { it.ready } &&
            (!transactionLists.historyOpen || transactionLists.history.ready) &&
            (subscriptionId == null || (payments.ready && transactions.ready))
}

internal data class TransactionListsState(
    val recent: HorizonSection<HorizonTransactionPage> = HorizonSection(),
    val history: HorizonSection<HorizonTransactionPage> = HorizonSection(),
    val historyOpen: Boolean = false,
    val historyFilters: HistoryFilters = HistoryFilters(),
    val historyRequest: HistoryRequest = HistoryRequest.Refresh,
    val selected: TransactionSelection? = null,
)

internal data class HistoryFilters(
    val search: String = "",
    val type: String? = null,
    val categoryId: Long? = null,
)

internal enum class HistoryRequest { Refresh, Append }

internal data class TransactionSelection(
    val id: Long,
    val source: TransactionList,
)

internal enum class TransactionList { Recent, History }

internal sealed interface HorizonConfirmation {
    data object Discard : HorizonConfirmation

    data class Apply(
        val change: HorizonChange,
        val message: UiText,
    ) : HorizonConfirmation
}

private val HorizonSection<*>.ready: Boolean get() = !loading && error == null && value != null
