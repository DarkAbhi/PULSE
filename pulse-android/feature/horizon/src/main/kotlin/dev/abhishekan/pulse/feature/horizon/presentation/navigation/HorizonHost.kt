package dev.abhishekan.pulse.feature.horizon.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.SaveableStateHolder
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
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import dev.abhishekan.pulse.core.ui.patterns.PulseScreenScaffold
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.accounts.AccountType
import dev.abhishekan.pulse.feature.horizon.presentation.accounts.AccountsTabRoute
import dev.abhishekan.pulse.feature.horizon.presentation.accounts.BankAccountsScreen
import dev.abhishekan.pulse.feature.horizon.presentation.accounts.CreditCardsScreen
import dev.abhishekan.pulse.feature.horizon.presentation.budgets.BudgetEditorScreen
import dev.abhishekan.pulse.feature.horizon.presentation.categories.CategoryEditorScreen
import dev.abhishekan.pulse.feature.horizon.presentation.components.horizonGutter
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonDraft
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorAction
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorState
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonForm
import dev.abhishekan.pulse.feature.horizon.presentation.editor.draft
import dev.abhishekan.pulse.feature.horizon.presentation.editor.topBarTitle
import dev.abhishekan.pulse.feature.horizon.presentation.obligations.FixedObligationEditorScreen
import dev.abhishekan.pulse.feature.horizon.presentation.purchases.PlannedPurchaseEditorScreen
import dev.abhishekan.pulse.feature.horizon.presentation.settings.HorizonSettingsEditorScreen
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonAction
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonConfirmation
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonTab
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonUiState
import dev.abhishekan.pulse.feature.horizon.presentation.state.TransactionList
import dev.abhishekan.pulse.feature.horizon.presentation.subscriptions.SubscriptionEditorScreen
import dev.abhishekan.pulse.feature.horizon.presentation.subscriptions.SubscriptionPaymentsSheet
import dev.abhishekan.pulse.feature.horizon.presentation.subscriptions.SubscriptionPaymentsState
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.TransactionDetails
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.editor.TransactionEditorScreen
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.history.TransactionHistoryScreen
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.history.TransactionHistoryUiState

@Composable
internal fun HorizonHost(
    state: HorizonUiState,
    scrollStates: SaveableStateHolder,
    active: Boolean,
    onAction: (HorizonAction) -> Unit,
) {
    val destination = state.destination
    val editor = destination as? HorizonDestination.Editor
    val records = state.showingReviewRecords
    BackHandler(active && (destination !is HorizonDestination.Tabs || state.busy || state.subscriptionId != null)) {
        onAction(HorizonAction.Back)
    }
    val currency = UserCurrency.code
    val canMutate = !state.busy && !state.reviewRequired && state.overview.value != null && state.draft == null
    val review: @Composable () -> Unit = {
        if (state.reviewRequired) {
            HorizonRecoveryNotice(
                hasDraft = editor != null,
                showingRecords = records,
                reviewReady = state.reviewReady && !state.busy,
                onRefresh = { onAction(HorizonAction.Refresh) },
                onToggleRecords = { onAction(HorizonAction.ToggleReviewRecords) },
                onAcknowledge = { onAction(HorizonAction.AcknowledgeReview) },
            )
        }
    }
    val backLabel = stringResource(editor?.parent?.backLabel ?: R.string.pulse_horizon_back_to_horizon)
    val draft = editor?.draft
    if (destination == HorizonDestination.BankAccounts) {
        BankAccountsScreen(currency, active)
    } else if (destination == HorizonDestination.CreditCards) {
        CreditCardsScreen(currency, active)
    } else if (draft != null && !records) {
        key(draft.kind, draft.id) {
            HorizonEditorDestination(draft, state, onAction, backLabel, review)
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
                                { onAction(HorizonAction.ResumeDraft) },
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
                                        history = state.transactionLists.history,
                                        overview = state.overview,
                                        historySearch = state.transactionLists.historyFilters.search,
                                        historyType = state.transactionLists.historyFilters.type,
                                        historyCategoryId = state.transactionLists.historyFilters.categoryId,
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
                                onAction = { onAction(HorizonAction.History(it)) },
                            ) {
                                review()
                                resumeDraft()
                            }
                        }
                    } else if (recordsDestination is HorizonDestination.Tabs && recordsDestination.tab == HorizonTab.Accounts) {
                        AccountsTabRoute(
                            enabled = !state.busy,
                            width = width,
                            header = {
                                HorizonTabs(HorizonTab.Accounts, !state.busy, width) { onAction(HorizonAction.SelectTab(it)) }
                            },
                            notice = {
                                review()
                                resumeDraft()
                            },
                            onOpen = { type ->
                                onAction(
                                    when (type) {
                                        AccountType.Bank -> HorizonAction.OpenBankAccounts
                                        AccountType.CreditCard -> HorizonAction.OpenCreditCards
                                    },
                                )
                            },
                        )
                    } else if (recordsDestination is HorizonDestination.Tabs) {
                        HorizonTabs(recordsDestination.tab, !state.busy, width) { onAction(HorizonAction.SelectTab(it)) }
                        HorizonTabContent(
                            tab = recordsDestination.tab,
                            state = state,
                            onAction = onAction,
                            width = width,
                            currency = currency,
                            canMutate = canMutate,
                            active = active,
                            reviewingDraft = draft != null,
                            scrollStates = scrollStates,
                        ) {
                            review()
                            resumeDraft()
                            if (records && state.transactionDraft != null && state.reviewRequired) {
                                HorizonCategoryRecords(state.overview) { onAction(HorizonAction.RetryOverview) }
                            }
                        }
                    }
                }
            }
        }
    }
    if (active && state.tab == HorizonTab.Subscriptions && state.subscriptionId != null && state.draft == null) {
        SubscriptionPaymentsSheet(
            SubscriptionPaymentsState(
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
            ),
        ) { onAction(HorizonAction.Payments(it)) }
    }
    val lists = state.transactionLists
    val source = if (lists.historyOpen) TransactionList.History else TransactionList.Recent
    val displayedTransactions = if (source == TransactionList.History) lists.history.value else lists.recent.value
    val selectedId = lists.selected?.takeIf { it.source == source }?.id
    if (active && state.draft == null && state.tab == HorizonTab.Transactions) {
        displayedTransactions?.items?.find { it.id == selectedId }?.let { tx ->
            TransactionDetails(
                tx,
                currency,
                canMutate,
                { onAction(HorizonAction.DismissTransaction) },
                { onAction(HorizonAction.Edit(it.draft(currency))) },
                { transaction ->
                    onAction(
                        HorizonAction.AskConfirmation(
                            HorizonChange.Delete(HorizonRecord.Transaction, transaction.id),
                            uiText(R.string.pulse_horizon_delete_1_s_this_permanently_removes_the_transaction_and, transaction.name),
                        ),
                    )
                },
            )
        }
    }
    if (active && state.confirmation == HorizonConfirmation.Discard) {
        PulseDialog(
            stringResource(R.string.pulse_horizon_discard_unsaved_changes),
            stringResource(R.string.pulse_horizon_your_edits_have_not_been_saved_leaving_this_form),
            stringResource(R.string.pulse_horizon_discard_edits),
            { onAction(HorizonAction.AcceptConfirmation) },
            { onAction(HorizonAction.DismissConfirmation) },
        )
    }
    (state.confirmation as? HorizonConfirmation.Apply)?.takeIf { active }?.let { (change, body) ->
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
            { onAction(HorizonAction.AcceptConfirmation) },
            { onAction(HorizonAction.DismissConfirmation) },
        )
    }
}

@Composable
private fun HorizonEditorDestination(
    draft: HorizonDraft,
    state: HorizonUiState,
    onAction: (HorizonAction) -> Unit,
    backLabel: String,
    review: @Composable () -> Unit,
) {
    val editor = HorizonEditorState(draft, state.overview.value, state.busy, state.formError, state.reviewRequired)
    val editorAction: (HorizonEditorAction) -> Unit = { onAction(HorizonAction.Editor(it)) }
    val notice: @Composable () -> Unit = {
        review()
        state.overview.error?.let { PulseErrorState(it.message, { onAction(HorizonAction.RetryOverview) }) }
    }
    when (draft.kind) {
        HorizonForm.Budget -> BudgetEditorScreen(editor, backLabel, editorAction, notice)
        HorizonForm.Transaction -> TransactionEditorScreen(editor, backLabel, editorAction, notice)
        HorizonForm.Subscription -> SubscriptionEditorScreen(editor, backLabel, editorAction, notice)
        HorizonForm.Deduction -> FixedObligationEditorScreen(editor, backLabel, editorAction, notice)
        HorizonForm.Category -> CategoryEditorScreen(editor, backLabel, editorAction, notice)
        HorizonForm.Config -> HorizonSettingsEditorScreen(editor, backLabel, editorAction, notice)
        HorizonForm.Purchase -> PlannedPurchaseEditorScreen(editor, backLabel, editorAction, notice)
    }
}
