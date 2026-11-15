package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.pagePadding

@Composable
internal fun HorizonTabScroll(
    padding: Dp,
    bottomSpacing: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = LocalPulseContentPadding.current.calculateBottomPadding() + bottomSpacing)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
    ) {
        content()
    }
}

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
