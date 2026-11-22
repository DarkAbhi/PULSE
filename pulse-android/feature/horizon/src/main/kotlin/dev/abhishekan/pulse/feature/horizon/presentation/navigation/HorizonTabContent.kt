package dev.abhishekan.pulse.feature.horizon.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.patterns.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.patterns.PulseLoadingState
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.budgets.BudgetsTabScreen
import dev.abhishekan.pulse.feature.horizon.presentation.budgets.BudgetsTabState
import dev.abhishekan.pulse.feature.horizon.presentation.components.HorizonTabScroll
import dev.abhishekan.pulse.feature.horizon.presentation.obligations.ObligationsTabScreen
import dev.abhishekan.pulse.feature.horizon.presentation.obligations.ObligationsTabState
import dev.abhishekan.pulse.feature.horizon.presentation.overview.HorizonOverviewTabScreen
import dev.abhishekan.pulse.feature.horizon.presentation.overview.HorizonOverviewTabState
import dev.abhishekan.pulse.feature.horizon.presentation.purchases.PlannedPurchasesTabScreen
import dev.abhishekan.pulse.feature.horizon.presentation.purchases.PlannedPurchasesTabState
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonAction
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonSection
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonTab
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonUiState
import dev.abhishekan.pulse.feature.horizon.presentation.subscriptions.SubscriptionsTabScreen
import dev.abhishekan.pulse.feature.horizon.presentation.subscriptions.SubscriptionsTabState
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.recent.RecentTransactionsTabScreen
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.recent.RecentTransactionsUiState

@Composable
internal fun HorizonTabContent(
    tab: HorizonTab,
    state: HorizonUiState,
    onAction: (HorizonAction) -> Unit,
    width: PulseWindowSize,
    currency: String,
    canMutate: Boolean,
    active: Boolean,
    reviewingDraft: Boolean,
    scrollStates: SaveableStateHolder,
    recovery: @Composable () -> Unit,
) {
    val scrollKey = (if (reviewingDraft) "review-" else "root-") + tab.name
    val saving: @Composable () -> Unit = {
        if (state.busy) PulseLoadingState(stringResource(R.string.pulse_horizon_saving_your_change))
    }
    if (tab == HorizonTab.Accounts) return
    if (tab == HorizonTab.Transactions) {
        RecentTransactionsTabScreen(
            state =
                RecentTransactionsUiState(
                    section = state.transactionLists.recent,
                    canOpenHistory = !state.busy && !reviewingDraft,
                    showAdd = active && !state.busy && !reviewingDraft,
                    reserveAddSpace = !reviewingDraft,
                    showFullEmptyState = !reviewingDraft && !state.reviewRequired,
                    canLog = canMutate,
                ),
            currency = currency,
            width = width,
            scrollKey = scrollKey,
            scrollStates = scrollStates,
            onAction = { onAction(HorizonAction.Recent(it)) },
            recovery = recovery,
            saving = saving,
        )
        return
    }

    val loading =
        when (tab) {
            HorizonTab.Purchases -> state.purchases.loading && state.purchases.value == null
            else -> state.overview.loading && state.overview.value == null
        }
    if (loading && !reviewingDraft && !state.reviewRequired) {
        Box(Modifier.fillMaxSize()) {
            PulseCenteredLoadingState(
                stringResource(R.string.pulse_horizon_loading_your_horizon),
                Modifier.fillMaxSize(),
            )
        }
        return
    }
    scrollStates.SaveableStateProvider(scrollKey) {
        HorizonTabScroll(width.pagePadding) {
            recovery()
            when (tab) {
                HorizonTab.Accounts -> Unit
                HorizonTab.Overview ->
                    HorizonOverviewTabScreen(HorizonOverviewTabState(state.overview, canMutate)) { onAction(HorizonAction.Overview(it)) }
                HorizonTab.Transactions -> Unit
                HorizonTab.Budgets ->
                    BudgetsTabScreen(
                        BudgetsTabState(
                            HorizonSection(state.overview.value?.budgets, state.overview.loading, state.overview.error),
                            currency,
                            canMutate,
                        ),
                    ) { onAction(HorizonAction.Budgets(it)) }
                HorizonTab.Obligations ->
                    ObligationsTabScreen(
                        ObligationsTabState(state.overview, canMutate, currency),
                    ) { onAction(HorizonAction.Obligations(it)) }
                HorizonTab.Subscriptions ->
                    SubscriptionsTabScreen(SubscriptionsTabState(state.overview, canMutate, currency, canViewPayments = !state.busy)) {
                        onAction(HorizonAction.Subscriptions(it))
                    }
                HorizonTab.Purchases ->
                    PlannedPurchasesTabScreen(PlannedPurchasesTabState(state.purchases, currency, canMutate)) {
                        onAction(HorizonAction.Purchases(it))
                    }
            }

            saving()
        }
    }
}
