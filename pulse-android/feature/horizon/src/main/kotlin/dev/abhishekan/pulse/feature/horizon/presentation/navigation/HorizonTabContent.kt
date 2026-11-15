package dev.abhishekan.pulse.feature.horizon.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.accounts.AccountsTabScreen
import dev.abhishekan.pulse.feature.horizon.presentation.budgets.BudgetsTabScreen
import dev.abhishekan.pulse.feature.horizon.presentation.components.HorizonTabScroll
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonForm
import dev.abhishekan.pulse.feature.horizon.presentation.obligations.ObligationsTabScreen
import dev.abhishekan.pulse.feature.horizon.presentation.overview.HorizonOverviewTabScreen
import dev.abhishekan.pulse.feature.horizon.presentation.purchases.PlannedPurchasesTabScreen
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonTab
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonUiState
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonViewModel
import dev.abhishekan.pulse.feature.horizon.presentation.subscriptions.SubscriptionsTabScreen
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.recent.RecentTransactionsTabScreen

@Composable
internal fun HorizonTabContent(
    tab: HorizonTab,
    state: HorizonUiState,
    vm: HorizonViewModel,
    width: PulseWindowSize,
    currency: String,
    canMutate: Boolean,
    active: Boolean,
    reviewingDraft: Boolean,
    scrollStates: SaveableStateHolder,
    onSelectTransaction: (Long) -> Unit,
    onImport: () -> Unit,
    confirm: (HorizonChange, UiText) -> Unit,
    recovery: @Composable () -> Unit,
) {
    val scrollKey = (if (reviewingDraft) "review-" else "root-") + tab.name
    val saving: @Composable () -> Unit = {
        if (state.busy) PulseLoadingState(stringResource(R.string.pulse_horizon_saving_your_change))
    }
    if (tab == HorizonTab.Transactions) {
        RecentTransactionsTabScreen(
            section = state.recent,
            currency = currency,
            width = width,
            canOpenHistory = !state.busy && !reviewingDraft,
            onRetry = vm::refreshRecent,
            onOpenHistory = vm::openHistory,
            onSelect = onSelectTransaction,
            showAdd = active && !state.busy && !reviewingDraft,
            reserveAddSpace = !reviewingDraft,
            showFullEmptyState = !reviewingDraft && !state.reviewRequired,
            canLog = canMutate,
            onLog = { vm.newForm(HorizonForm.Transaction) },
            onImport = onImport,
            scrollKey = scrollKey,
            scrollStates = scrollStates,
            recovery = recovery,
            saving = saving,
        )
        return
    }

    val loading =
        when (tab) {
            HorizonTab.Accounts -> false
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
                HorizonTab.Accounts -> AccountsTabScreen(currency)
                HorizonTab.Overview ->
                    HorizonOverviewTabScreen(
                        state.overview,
                        canMutate,
                        vm::refreshOverview,
                    ) { vm.newForm(HorizonForm.Config) }
                HorizonTab.Transactions -> Unit
                HorizonTab.Budgets ->
                    BudgetsTabScreen(
                        section = state.overview,
                        canMutate = canMutate,
                        onRetry = vm::refreshOverview,
                        onAdd = { vm.newForm(HorizonForm.Budget) },
                        currency = currency,
                        onEdit = vm::edit,
                        onConfirm = confirm,
                    )
                HorizonTab.Obligations ->
                    ObligationsTabScreen(
                        section = state.overview,
                        canMutate = canMutate,
                        onRetry = vm::refreshOverview,
                        onAdd = { vm.newForm(HorizonForm.Deduction) },
                        currency = currency,
                        onEdit = vm::edit,
                        onConfirm = confirm,
                    )
                HorizonTab.Subscriptions ->
                    SubscriptionsTabScreen(
                        section = state.overview,
                        canMutate = canMutate,
                        onRetry = vm::refreshOverview,
                        onAdd = { vm.newForm(HorizonForm.Subscription) },
                        currency = currency,
                        canViewPayments = !state.busy,
                        onEdit = vm::edit,
                        onConfirm = confirm,
                        onViewPayments = vm::showPayments,
                    )
                HorizonTab.Purchases ->
                    PlannedPurchasesTabScreen(
                        section = state.purchases,
                        currency = currency,
                        canMutate = canMutate,
                        onRetry = vm::refreshPurchases,
                        onAdd = { vm.newForm(HorizonForm.Purchase) },
                        onConfirm = confirm,
                    )
            }

            saving()
        }
    }
}
