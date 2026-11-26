package dev.abhishekan.pulse.feature.horizon.presentation.state

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.HorizonRecord
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.domain.repository.HorizonRepository
import dev.abhishekan.pulse.core.domain.usecase.ApplyHorizonChange
import dev.abhishekan.pulse.core.domain.usecase.GetHorizonTransactions
import dev.abhishekan.pulse.core.domain.usecase.GetSubscriptionPayments
import dev.abhishekan.pulse.core.ui.DraftSource
import dev.abhishekan.pulse.core.ui.UnsavedChanges
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.accounts.AccountType
import dev.abhishekan.pulse.feature.horizon.presentation.budgets.BudgetsTabAction
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonDraft
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorAction
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonForm
import dev.abhishekan.pulse.feature.horizon.presentation.obligations.ObligationsTabAction
import dev.abhishekan.pulse.feature.horizon.presentation.overview.HorizonOverviewTabAction
import dev.abhishekan.pulse.feature.horizon.presentation.purchases.PlannedPurchasesTabAction
import dev.abhishekan.pulse.feature.horizon.presentation.subscriptions.SubscriptionPaymentsAction
import dev.abhishekan.pulse.feature.horizon.presentation.subscriptions.SubscriptionsTabAction
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.history.TransactionHistoryAction
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.recent.RecentTransactionsAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DateTimeException
import java.time.ZonedDateTime

class HorizonViewModel(
    private val horizon: HorizonRepository,
    private val getTransactions: GetHorizonTransactions,
    private val getPayments: GetSubscriptionPayments,
    private val apply: ApplyHorizonChange,
    private val saved: SavedStateHandle,
    unsavedChanges: UnsavedChanges = UnsavedChanges(),
) : ViewModel() {
    private val state =
        MutableStateFlow(
            HorizonUiState(
                tab = HorizonTab.valueOf(saved["tab"] ?: "Overview"),
                search = saved["search"] ?: "",
                type = saved["type"],
                transactionLists =
                    TransactionListsState(
                        historyOpen = saved["historyOpen"] ?: false,
                        historyFilters = HistoryFilters(saved["historySearch"] ?: "", saved["historyType"], saved["historyCategoryId"]),
                        selected = restoreTransactionSelection(),
                    ),
                importOpen = saved["importOpen"] ?: false,
                accountList = AccountType.entries.find { it.name == saved.get<String>("accountList") },
                transactionDraft = saved.get<ArrayList<String>>("transactionDraft")?.let(HorizonDraft::restore),
                subscriptionId = saved["subscription"],
                draft = saved.get<ArrayList<String>>("draft")?.let(HorizonDraft::restore),
                reviewRequired =
                    saved["pending"] ?: false,
                showingReviewRecords = saved["showingReviewRecords"] ?: false,
                confirmation = if (saved.get<Boolean>("confirmDiscard") == true) HorizonConfirmation.Discard else null,
            ),
        )
    internal val uiState = state.asStateFlow()

    internal fun onAction(action: HorizonAction) {
        when (action) {
            HorizonAction.Refresh -> refresh()
            HorizonAction.RetryOverview -> refreshOverview()
            HorizonAction.ConsumeMessage -> consumeMessage()
            is HorizonAction.SelectTab -> tab(action.tab)
            is HorizonAction.Recent -> onRecentAction(action.action)
            is HorizonAction.History -> onHistoryAction(action.action)
            is HorizonAction.Overview -> onOverviewAction(action.action)
            is HorizonAction.Budgets -> onBudgetsAction(action.action)
            is HorizonAction.Obligations -> onObligationsAction(action.action)
            is HorizonAction.Subscriptions -> onSubscriptionsAction(action.action)
            is HorizonAction.Purchases -> onPurchasesAction(action.action)
            is HorizonAction.Payments -> onPaymentsAction(action.action)
            HorizonAction.DismissTransaction -> selectTransaction(null)
            HorizonAction.CloseImport -> setImportOpen(false)
            HorizonAction.ManageAccounts -> {
                setImportOpen(false)
                tab(HorizonTab.Accounts)
            }
            HorizonAction.ImportCompleted -> completeImport()
            HorizonAction.OpenBankAccounts -> openAccountList(AccountType.Bank)
            HorizonAction.OpenCreditCards -> openAccountList(AccountType.CreditCard)
            HorizonAction.Back -> back()
            HorizonAction.ToggleReviewRecords -> showReviewRecords(!state.value.showingReviewRecords)
            HorizonAction.ResumeDraft -> showReviewRecords(false)
            HorizonAction.AcknowledgeReview -> acknowledgeReview()
            is HorizonAction.Edit -> edit(action.draft)
            is HorizonAction.Editor -> onEditorAction(action.action)
            is HorizonAction.AskConfirmation -> confirm(HorizonConfirmation.Apply(action.change, action.message))
            HorizonAction.AcceptConfirmation -> acceptConfirmation()
            HorizonAction.DismissConfirmation -> confirm(null)
        }
    }

    private fun onOverviewAction(action: HorizonOverviewTabAction) {
        when (action) {
            HorizonOverviewTabAction.Retry -> refreshOverview()
            HorizonOverviewTabAction.EditSettings -> newForm(HorizonForm.Config)
        }
    }

    private fun onBudgetsAction(action: BudgetsTabAction) {
        when (action) {
            BudgetsTabAction.Retry -> refreshOverview()
            BudgetsTabAction.Add -> newForm(HorizonForm.Budget)
            is BudgetsTabAction.Edit ->
                edit(
                    HorizonDraft(
                        HorizonForm.Budget,
                        action.budget.id,
                        action.budget.name,
                        action.budget.allocated.toPlainString(),
                        UserCurrency.code,
                    ),
                )
            is BudgetsTabAction.Delete ->
                requestDelete(HorizonRecord.Budget, action.budget.id, R.string.pulse_horizon_delete_budget_confirmation, action.budget.name)
        }
    }

    private fun onObligationsAction(action: ObligationsTabAction) {
        when (action) {
            ObligationsTabAction.Retry -> refreshOverview()
            ObligationsTabAction.Add -> newForm(HorizonForm.Deduction)
            is ObligationsTabAction.Edit -> edit(action.draft)
            is ObligationsTabAction.RequestDelete ->
                requestDelete(
                    HorizonRecord.Deduction,
                    action.id,
                    R.string.pulse_horizon_delete_1_s_this_removes_the_fixed_obligation_and,
                    action.name,
                )
        }
    }

    private fun onSubscriptionsAction(action: SubscriptionsTabAction) {
        when (action) {
            SubscriptionsTabAction.Retry -> refreshOverview()
            SubscriptionsTabAction.Add -> newForm(HorizonForm.Subscription)
            is SubscriptionsTabAction.Edit -> edit(action.draft)
            is SubscriptionsTabAction.RequestDelete ->
                requestDelete(HorizonRecord.Subscription, action.id, R.string.pulse_horizon_delete_subscription_confirmation, action.name)
            is SubscriptionsTabAction.ViewPayments -> showPayments(action.id)
        }
    }

    private fun onPurchasesAction(action: PlannedPurchasesTabAction) {
        when (action) {
            PlannedPurchasesTabAction.Retry -> refreshPurchases()
            PlannedPurchasesTabAction.Add -> newForm(HorizonForm.Purchase)
            is PlannedPurchasesTabAction.Delete ->
                requestDelete(
                    HorizonRecord.Purchase,
                    action.purchase.id,
                    R.string.pulse_horizon_remove_1_s_from_next_month_s_planned_purchases,
                    action.purchase.name,
                )
            PlannedPurchasesTabAction.Clear ->
                confirm(
                    HorizonConfirmation.Apply(
                        HorizonChange.ClearPurchases,
                        uiText(R.string.pulse_horizon_permanently_remove_every_purchase_in_next_month_s_list),
                    ),
                )
        }
    }

    private fun onPaymentsAction(action: SubscriptionPaymentsAction) {
        when (action) {
            SubscriptionPaymentsAction.Dismiss -> hidePayments()
            SubscriptionPaymentsAction.RetryPayments -> state.value.subscriptionId?.let(::showPayments)
            SubscriptionPaymentsAction.RetryTransactions -> refreshTransactions()
            is SubscriptionPaymentsAction.Filter -> filter(action.search, action.type)
            is SubscriptionPaymentsAction.Page -> refreshTransactions(action.page)
            is SubscriptionPaymentsAction.RequestLink -> {
                val subscriptionId = state.value.subscriptionId ?: return
                confirm(
                    HorizonConfirmation.Apply(
                        HorizonChange.LinkPayment(subscriptionId, action.id),
                        uiText(R.string.pulse_horizon_link_this_debit_to_the_subscription_the_transaction_remains),
                    ),
                )
            }
        }
    }

    private fun requestDelete(
        record: HorizonRecord,
        id: Long,
        message: Int,
        name: String,
    ) {
        confirm(HorizonConfirmation.Apply(HorizonChange.Delete(record, id), uiText(message, name)))
    }

    private fun onEditorAction(action: HorizonEditorAction) {
        when (action) {
            is HorizonEditorAction.Change -> update(action.draft)
            HorizonEditorAction.Save -> save()
            HorizonEditorAction.Cancel -> requestCancelEdit()
            HorizonEditorAction.AddCategory -> addTransactionCategory()
            is HorizonEditorAction.DeleteObligation -> deleteObligation(action.id)
        }
    }

    private fun onRecentAction(action: RecentTransactionsAction) {
        when (action) {
            RecentTransactionsAction.Retry -> refreshRecent()
            RecentTransactionsAction.OpenHistory -> openHistory()
            is RecentTransactionsAction.Select -> selectTransaction(TransactionSelection(action.id, TransactionList.Recent))
            RecentTransactionsAction.Log -> newForm(HorizonForm.Transaction)
            RecentTransactionsAction.Import -> setImportOpen(true)
        }
    }

    private fun onHistoryAction(action: TransactionHistoryAction) {
        when (action) {
            TransactionHistoryAction.Back -> closeHistory()
            is TransactionHistoryAction.Filter -> filterHistory(action.search, action.type, action.categoryId)
            TransactionHistoryAction.RetryCategories -> refreshOverview()
            TransactionHistoryAction.Retry -> retryHistory()
            TransactionHistoryAction.LoadMore -> loadMoreHistory()
            is TransactionHistoryAction.Select -> selectTransaction(TransactionSelection(action.id, TransactionList.History))
            TransactionHistoryAction.Log -> newForm(HorizonForm.Transaction)
        }
    }

    private fun restoreTransactionSelection(): TransactionSelection? {
        val id = saved.get<Long>("selectedTransaction") ?: return null
        val source = TransactionList.entries.find { it.name == saved.get<String>("selectedTransactionSource") } ?: return null
        return TransactionSelection(id, source)
    }

    private fun selectTransaction(selection: TransactionSelection?) {
        saved["selectedTransaction"] = selection?.id
        saved["selectedTransactionSource"] = selection?.source?.name
        state.update { it.copy(transactionLists = it.transactionLists.copy(selected = selection)) }
    }

    private fun back() {
        val current = state.value
        when {
            current.busy -> Unit
            current.accountList != null -> {
                saved["accountList"] = null
                state.update { it.copy(accountList = null) }
            }
            current.draft != null -> requestCancelEdit()
            current.transactionLists.historyOpen -> closeHistory()
            else -> hidePayments()
        }
    }

    private fun showReviewRecords(show: Boolean) {
        saved["showingReviewRecords"] = show
        state.update { it.copy(showingReviewRecords = show) }
    }

    private fun requestCancelEdit() {
        if (state.value.busy) return
        if (state.value.draft?.dirty == true) confirm(HorizonConfirmation.Discard) else discard()
    }

    private fun confirm(confirmation: HorizonConfirmation?) {
        saved["confirmDiscard"] = confirmation == HorizonConfirmation.Discard
        state.update { it.copy(confirmation = confirmation) }
    }

    private fun acceptConfirmation() {
        val pending = state.value.confirmation
        confirm(null)
        when (pending) {
            HorizonConfirmation.Discard -> discard()
            is HorizonConfirmation.Apply -> mutate(pending.change)
            null -> Unit
        }
    }

    private fun completeImport() {
        if (!state.value.importOpen) return
        setImportOpen(false)
        refresh()
        state.update { it.copy(message = uiText(R.string.pulse_horizon_statement_complete)) }
    }

    init {
        unsavedChanges.track(
            DraftSource.Horizon,
            state.map { it.draft?.dirty == true || it.transactionDraft?.dirty == true },
            viewModelScope,
        )
    }

    private fun deleteObligation(id: Long) = mutate(HorizonChange.Delete(HorizonRecord.Deduction, id), closeDraft = true)

    private fun consumeMessage() {
        state.update { it.copy(message = null) }
    }

    private val jobs = mutableMapOf<String, Job>()

    init {
        refresh()
    }

    private fun <T> fetch(
        key: String,
        block: suspend () -> Outcome<T>,
        current: (HorizonUiState) -> HorizonSection<T>,
        update: (HorizonUiState, HorizonSection<T>) -> HorizonUiState,
    ) {
        jobs[key]?.cancel()
        state.update { update(it, current(it).copy(loading = true, error = null)) }
        jobs[key] =
            viewModelScope.launch {
                val result =
                    when (val value = block()) {
                        is Outcome.Success -> HorizonSection(value.value, loading = false)
                        is Outcome.Error -> current(state.value).copy(loading = false, error = value.failure)
                    }
                ensureActive()
                state.update { update(it, result) }
            }
    }

    private fun refresh() {
        refreshOverview()
        refreshRecent()
        if (state.value.transactionLists.historyOpen) refreshHistory()
        refreshPurchases()
        state.value.subscriptionId?.let(::showPayments)
    }

    private fun refreshOverview() =
        fetch("overview", { horizon.overview() }, { it.overview }) { s, v ->
            if (v.value != null &&
                s.subscriptionId != null &&
                v.value.subscriptions.none { it.id == s.subscriptionId }
            ) {
                saved["subscription"] = null
                s.copy(overview = v, subscriptionId = null, payments = HorizonSection(loading = false))
            } else {
                s.copy(overview = v)
            }
        }

    private fun refreshPurchases() =
        fetch("purchases", {
            horizon.purchases()
        }, { it.purchases }) { s, v -> s.copy(purchases = v) }

    private fun refreshRecent() =
        fetch("recent", { getTransactions(1, "", null, pageSize = 5) }, { it.transactionLists.recent }) { s, v ->
            s.copy(transactionLists = s.transactionLists.copy(recent = v))
        }

    private fun openHistory() {
        selectTransaction(null)
        saved["historyOpen"] = true
        state.update { it.copy(transactionLists = it.transactionLists.copy(historyOpen = true)) }
        refreshHistory()
    }

    private fun closeHistory() {
        jobs["history"]?.cancel()
        selectTransaction(null)
        saved["historyOpen"] = false
        state.update { it.copy(transactionLists = it.transactionLists.copy(historyOpen = false, history = HorizonSection())) }
    }

    private fun filterHistory(
        search: String,
        type: String?,
        categoryId: Long?,
    ) {
        val current = state.value
        if (!current.transactionLists.historyOpen || current.busy) return
        val filters = HistoryFilters(search, type, categoryId)
        if (filters == current.transactionLists.historyFilters) return
        saved["historySearch"] = search
        saved["historyType"] = type
        saved["historyCategoryId"] = categoryId
        state.update {
            it.copy(transactionLists = it.transactionLists.copy(historyFilters = filters, history = HorizonSection()))
        }
        refreshHistory()
    }

    private fun refreshHistory() {
        loadHistory(HistoryRequest.Refresh)
    }

    private fun loadMoreHistory() {
        val history = state.value.transactionLists.history
        val page = history.value ?: return
        if (!history.loading && page.page < page.totalPages) loadHistory(HistoryRequest.Append)
    }

    private fun retryHistory() = loadHistory(state.value.transactionLists.historyRequest)

    private fun loadHistory(request: HistoryRequest) {
        val current = state.value.transactionLists
        if (!current.historyOpen) return
        val append = request == HistoryRequest.Append
        val filters = current.historyFilters
        val previous = current.history.value
        val first = if (append && previous != null) previous.page + 1 else 1
        val last = if (append) first else previous?.page ?: 1
        fetch(
            "history",
            {
                val items = if (append) previous?.items.orEmpty().toMutableList() else mutableListOf()
                for (number in first..last) {
                    when (val result = getTransactions(number, filters.search, filters.type, filters.categoryId)) {
                        is Outcome.Error -> return@fetch result
                        is Outcome.Success -> {
                            val page = result.value
                            if (page.page == number) items.addAll(page.items)
                            if (number == last || number >= page.totalPages) {
                                return@fetch Outcome.Success(page.copy(items = items.distinctBy { it.id }))
                            }
                        }
                    }
                }
                error("history page range is empty")
            },
            { it.transactionLists.history },
        ) { s, v -> s.copy(transactionLists = s.transactionLists.copy(history = v, historyRequest = request)) }
    }

    private fun refreshTransactions(page: Int = saved["page"] ?: 1) {
        if (saved.get<Int>("page") != page) {
            state.update { it.copy(transactions = HorizonSection()) }
        }
        saved["page"] = page
        val search = state.value.search
        val type = state.value.type
        fetch("transactions", { getTransactions(page, search, type) }, { it.transactions }) { s, v ->
            v.value?.let { saved["page"] = it.page }
            s.copy(transactions = v)
        }
    }

    private fun filter(
        search: String,
        type: String?,
    ) {
        if (state.value.busy) return
        saved["search"] = search
        saved["type"] = type
        saved["page"] = 1
        state.update {
            it.copy(
                search = search,
                type = type,
                transactions =
                    if (it.search != search || it.type != type || it.transactions.value?.page != 1) {
                        HorizonSection()
                    } else {
                        it.transactions
                    },
            )
        }
        refreshTransactions(1)
    }

    private fun setImportOpen(open: Boolean) {
        if (open) selectTransaction(null)
        saved["importOpen"] = open
        state.update { it.copy(importOpen = open) }
    }

    private fun tab(tab: HorizonTab) {
        if (state.value.tab != tab) selectTransaction(null)
        saved["tab"] = tab.name
        saved["accountList"] = null
        state.update { it.copy(tab = tab, accountList = null) }
    }

    private fun openAccountList(type: AccountType) {
        if (state.value.busy) return
        tab(HorizonTab.Accounts)
        saved["accountList"] = type.name
        state.update { it.copy(accountList = type) }
    }

    private fun showPayments(id: Long) {
        saved["subscription"] = id
        state.update {
            it.copy(subscriptionId = id, payments = if (it.subscriptionId == id) it.payments else HorizonSection())
        }
        refreshTransactions()
        fetch("payments", { getPayments(id) }, { it.payments }) { s, v -> s.copy(payments = v) }
    }

    private fun hidePayments() {
        saved["subscription"] = null
        jobs["payments"]?.cancel()
        jobs["transactions"]?.cancel()
        state.update { it.copy(subscriptionId = null, payments = HorizonSection(loading = false)) }
    }

    private fun edit(draft: HorizonDraft) {
        if (!state.value.busy &&
            !state.value.reviewRequired
        ) {
            selectTransaction(null)
            saved["draft"] = draft.saved()
            state.update { it.copy(draft = draft, formError = null, message = null) }
        }
    }

    private fun update(draft: HorizonDraft) {
        if (!state.value.busy) editStored(draft.copy(dirty = true))
    }

    private fun editStored(draft: HorizonDraft?) {
        saved["draft"] = draft?.saved()
        state.update { it.copy(draft = draft, formError = null) }
    }

    private fun discard() {
        if (!state.value.busy) closeForm()
    }

    private fun addTransactionCategory() {
        val current = state.value
        val draft = current.draft ?: return
        if (draft.kind != HorizonForm.Transaction || draft.id != null || current.busy || current.reviewRequired) return
        saved["transactionDraft"] = draft.saved()
        state.update { it.copy(transactionDraft = draft) }
        newForm(HorizonForm.Category)
    }

    private fun closeForm() {
        showReviewRecords(false)
        val parent = state.value.transactionDraft
        saved["transactionDraft"] = null
        saved["draft"] = parent?.saved()
        state.update { it.copy(draft = parent, transactionDraft = null, formError = null) }
    }

    private fun newForm(kind: HorizonForm) {
        val data = state.value.overview.value
        val now = ZonedDateTime.now(UserTimeZone.zone)
        edit(
            HorizonDraft(
                kind,
                currency = UserCurrency.code,
                amount =
                    if (kind ==
                        HorizonForm.Config
                    ) {
                        data?.baseAmount?.toPlainString().orEmpty()
                    } else {
                        ""
                    },
                date =
                    if (kind in listOf(HorizonForm.Transaction, HorizonForm.Subscription)) {
                        now.toLocalDate().toString()
                    } else {
                        ""
                    },
                dueDay = if (kind == HorizonForm.Subscription) now.dayOfMonth.toString() else "",
                categoryId =
                    if (kind == HorizonForm.Subscription) {
                        data?.categories?.find { it.isSubscriptionDefault }?.id
                    } else {
                        null
                    },
                time =
                    now
                        .toLocalTime()
                        .withSecond(0)
                        .withNano(0)
                        .toString(),
            ),
        )
    }

    private fun save() {
        if (state.value.busy || state.value.reviewRequired) return
        val draft = state.value.draft ?: return
        val change =
            try {
                draft.change()
            } catch (
                _: IllegalArgumentException,
            ) {
                null
            } catch (_: DateTimeException) {
                null
            }
        if (change == null || !apply.valid(change)) {
            state.update {
                it.copy(
                    formError =
                        uiText(
                            R.string.pulse_horizon_check_the_fields_a_name_is_required_amounts_use,
                        ),
                )
            }
            return
        }
        mutate(change, closeDraft = true)
    }

    private fun mutate(
        change: HorizonChange,
        closeDraft: Boolean = false,
    ) {
        if (state.value.busy || state.value.reviewRequired) return
        saved["pending"] = true
        state.update { it.copy(busy = true, formError = null, message = null) }
        viewModelScope.launch {
            when (val result = apply(change)) {
                is Outcome.Success -> {
                    saved["pending"] = false
                    if (closeDraft) closeForm()
                    state.update {
                        it.copy(
                            busy = false,
                            message =
                                uiText(
                                    R.string.pulse_horizon_saved_your_horizon_is_up_to_date,
                                ),
                        )
                    }
                }
                is Outcome.Error ->
                    state.update {
                        it.copy(
                            busy = false,
                            reviewRequired = true,
                        )
                    }
            }
            refresh()
        }
    }

    private fun acknowledgeReview() {
        if (state.value.reviewRequired && state.value.reviewReady) {
            saved["pending"] = false
            state.update {
                it.copy(
                    reviewRequired = false,
                    message =
                        uiText(
                            R.string.pulse_horizon_server_records_reviewed_discard_a_draft_if_its_change,
                        ),
                )
            }
        }
    }
}
