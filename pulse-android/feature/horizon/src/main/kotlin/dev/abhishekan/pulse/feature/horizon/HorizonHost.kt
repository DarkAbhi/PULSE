package dev.abhishekan.pulse.feature.horizon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.HorizonRecord
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseScreenScaffold
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText

@Composable
internal fun HorizonHost(
    state: HorizonUiState,
    vm: HorizonViewModel,
    scrollStates: SaveableStateHolder,
    active: Boolean,
    openImport: () -> Unit,
) {
    val destination = state.destination
    val editor = destination as? HorizonDestination.Editor
    var discard by rememberSaveable { mutableStateOf(false) }
    var records by rememberSaveable { mutableStateOf(false) }
    var deletion by remember { mutableStateOf<Pair<HorizonChange, UiText>?>(null) }
    var selectedTransaction by rememberSaveable { mutableStateOf<Long?>(null) }
    val closeForm = {
        records = false
        vm.discard()
    }
    val cancel = { if (state.draft?.dirty == true) discard = true else closeForm() }
    BackHandler(active && (destination !is HorizonDestination.Tabs || state.busy || state.subscriptionId != null)) {
        when {
            state.busy -> Unit
            editor != null -> cancel()
            destination == HorizonDestination.History -> vm.closeHistory()
            else -> vm.hidePayments()
        }
    }
    val currency = UserCurrency.code
    val canMutate = !state.busy && !state.reviewRequired && state.overview.value != null && state.draft == null
    val review: @Composable () -> Unit = {
        if (state.reviewRequired) {
            HorizonRecoveryNotice(
                hasDraft = editor != null,
                showingRecords = records,
                reviewReady = state.reviewReady && !state.busy,
                onRefresh = vm::refresh,
                onToggleRecords = { records = !records },
                onAcknowledge = vm::acknowledgeReview,
            )
        }
    }
    val backLabel = stringResource(editor?.parent?.backLabel ?: R.string.pulse_horizon_back_to_horizon)
    val draft = editor?.draft
    if (draft != null && !records) {
        key(draft.kind, draft.id) {
            HorizonEditorDestination(draft, state, vm, backLabel, cancel, review)
        }
    } else {
        PulseScreenScaffold(
            title = if (draft == null) "" else stringResource(draft.topBarTitle()),
            showTopBar = draft != null,
            backLabel = backLabel,
            backEnabled = !state.busy,
        ) {
            PulseAdaptiveLayout(Modifier.fillMaxSize()) { width ->
                Column(
                    Modifier
                        .widthIn(
                            max = PulseTokens.contentWidth,
                        ).fillMaxWidth()
                        .padding(top = LocalPulseContentPadding.current.calculateTopPadding())
                        .imePadding(),
                ) {
                    val resumeDraft: @Composable () -> Unit = {
                        if (draft != null && !state.reviewRequired) {
                            PulseButton(
                                stringResource(R.string.pulse_horizon_resume_unsaved_edits),
                                { records = false },
                                style = PulseButtonStyle.Text,
                            )
                        }
                    }
                    val recordsDestination = state.recordsDestination
                    if (recordsDestination == HorizonDestination.History) {
                        val bottom = LocalPulseContentPadding.current.calculateBottomPadding()
                        val wide = width == PulseWindowSize.Expanded && LocalDensity.current.fontScale <= 1.15f
                        scrollStates.SaveableStateProvider(if (state.draft == null) "transaction-history" else "review-history") {
                            TransactionHistoryScreen(
                                state =
                                    TransactionHistoryUiState(
                                        history = state.history,
                                        overview = state.overview,
                                        historySearch = state.historySearch,
                                        historyType = state.historyType,
                                        historyCategoryId = state.historyCategoryId,
                                        busy = state.busy,
                                        reviewingDraft = draft != null,
                                        reviewRequired = state.reviewRequired,
                                    ),
                                active = active && draft == null,
                                padding =
                                    PaddingValues(
                                        start = width.horizonGutter,
                                        end = width.horizonGutter,
                                        top = PulseTokens.small,
                                        bottom = bottom,
                                    ),
                                tablet = width != PulseWindowSize.Compact,
                                wide = wide,
                                canLog = canMutate,
                                select = { selectedTransaction = it.id },
                                onBack = vm::closeHistory,
                                onFilter = vm::filterHistory,
                                onRetryCategories = vm::refreshOverview,
                                onRetry = vm::retryHistory,
                                onLoadMore = vm::loadMoreHistory,
                                onLog = { vm.newForm(HorizonForm.Transaction) },
                            ) {
                                review()
                                resumeDraft()
                            }
                        }
                    } else if (recordsDestination is HorizonDestination.Tabs) {
                        HorizonTabs(recordsDestination.tab, !state.busy, width, vm::tab)
                        HorizonTabContent(
                            tab = recordsDestination.tab,
                            state = state,
                            vm = vm,
                            width = width,
                            currency = currency,
                            canMutate = canMutate,
                            active = active,
                            reviewingDraft = draft != null,
                            scrollStates = scrollStates,
                            onSelectTransaction = { selectedTransaction = it },
                            onImport = openImport,
                            confirm = { change, message -> deletion = change to message },
                        ) {
                            review()
                            resumeDraft()
                            if (records && state.transactionDraft != null && state.reviewRequired) {
                                HorizonCategoryRecords(state.overview, vm::refreshOverview)
                            }
                        }
                    }
                }
            }
        }
    }
    if (active && state.tab == HorizonTab.Subscriptions && state.subscriptionId != null && state.draft == null) {
        SubscriptionPaymentsSheet(
            subscriptionName =
                state.overview.value
                    ?.subscriptions
                    ?.find { it.id == state.subscriptionId }
                    ?.name,
            payments = state.payments,
            transactions = state.transactions,
            search = state.search,
            type = state.type,
            busy = state.busy,
            enabled = canMutate,
            onDismiss = vm::hidePayments,
            onRetryPayments = { vm.showPayments(state.subscriptionId) },
            onRetryTransactions = { vm.refreshTransactions() },
            onFilter = vm::filter,
            onPage = { vm.refreshTransactions(it) },
        ) { id ->
            deletion = HorizonChange.LinkPayment(state.subscriptionId, id) to
                uiText(R.string.pulse_horizon_link_this_debit_to_the_subscription_the_transaction_remains)
        }
    }
    val displayedTransactions = if (state.historyOpen) state.history.value else state.recent.value
    if (active && state.draft == null && state.tab == HorizonTab.Transactions) {
        displayedTransactions?.items?.find { it.id == selectedTransaction }?.let { tx ->
            TransactionDetails(
                tx,
                currency,
                canMutate,
                { selectedTransaction = null },
                { vm.edit(it.draft(currency)) },
                { transaction ->
                    deletion = HorizonChange.Delete(HorizonRecord.Transaction, transaction.id) to
                        uiText(R.string.pulse_horizon_delete_1_s_this_permanently_removes_the_transaction_and, transaction.name)
                },
            )
        }
    }
    if (active && discard) {
        PulseDialog(
            stringResource(R.string.pulse_horizon_discard_unsaved_changes),
            stringResource(R.string.pulse_horizon_your_edits_have_not_been_saved_leaving_this_form),
            stringResource(R.string.pulse_horizon_discard_edits),
            {
                discard = false
                closeForm()
            },
            { discard = false },
        )
    }
    deletion?.takeIf { active }?.let { (change, body) ->
        PulseDialog(
            if (change is HorizonChange.LinkPayment) {
                stringResource(
                    R.string.pulse_horizon_link_payment,
                )
            } else {
                stringResource(R.string.pulse_horizon_confirm_deletion)
            },
            body.asString(),
            if (change is HorizonChange.LinkPayment) {
                stringResource(
                    R.string.pulse_horizon_link_payment_action,
                )
            } else {
                stringResource(R.string.pulse_horizon_delete)
            },
            {
                deletion = null
                vm.mutate(change)
            },
            { deletion = null },
        )
    }
}

@Composable
private fun HorizonEditorDestination(
    draft: HorizonDraft,
    state: HorizonUiState,
    vm: HorizonViewModel,
    backLabel: String,
    cancel: () -> Unit,
    review: @Composable () -> Unit,
) {
    val data = state.overview.value
    val canSave = !state.reviewRequired && !state.busy && data != null
    val notice: @Composable () -> Unit = {
        review()
        state.overview.error?.let { PulseErrorState(it.message, vm::refreshOverview) }
    }
    when (draft.kind) {
        HorizonForm.Budget ->
            BudgetEditorScreen(
                draft = draft,
                busy = state.busy,
                error = state.formError,
                backLabel = backLabel,
                onChange = vm::update,
                onSave = vm::save,
                onCancel = cancel,
                notice = notice,
                canSave = canSave,
            )
        HorizonForm.Transaction ->
            TransactionEditorScreen(
                draft = draft,
                busy = state.busy,
                error = state.formError,
                backLabel = backLabel,
                onChange = vm::update,
                onSave = vm::save,
                onCancel = cancel,
                notice = notice,
                data = data,
                canSave = canSave,
                onAddCategory = vm::addTransactionCategory,
            )
        HorizonForm.Subscription ->
            SubscriptionEditorScreen(
                draft = draft,
                busy = state.busy,
                error = state.formError,
                backLabel = backLabel,
                onChange = vm::update,
                onSave = vm::save,
                onCancel = cancel,
                notice = notice,
                data = data,
                canSave = canSave,
            )
        HorizonForm.Deduction ->
            FixedObligationEditorScreen(
                draft = draft,
                busy = state.busy,
                error = state.formError,
                backLabel = backLabel,
                onChange = vm::update,
                onSave = vm::save,
                onCancel = cancel,
                notice = notice,
                data = data,
                canSave = canSave,
                onDelete = vm::deleteObligation,
            )
        HorizonForm.Category ->
            CategoryEditorScreen(
                draft = draft,
                busy = state.busy,
                error = state.formError,
                backLabel = backLabel,
                onChange = vm::update,
                onSave = vm::save,
                onCancel = cancel,
                notice = notice,
                canSave = canSave,
            )
        HorizonForm.Config ->
            HorizonSettingsEditorScreen(
                draft = draft,
                busy = state.busy,
                error = state.formError,
                backLabel = backLabel,
                onChange = vm::update,
                onSave = vm::save,
                onCancel = cancel,
                notice = notice,
                data = data,
                canSave = canSave,
            )
        HorizonForm.Purchase ->
            PlannedPurchaseEditorScreen(
                draft = draft,
                busy = state.busy,
                error = state.formError,
                backLabel = backLabel,
                onChange = vm::update,
                onSave = vm::save,
                onCancel = cancel,
                notice = notice,
                data = data,
                reviewRequired = state.reviewRequired,
            )
    }
}
