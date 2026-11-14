package dev.abhishekan.pulse.feature.horizon

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
                historyOpen = saved["historyOpen"] ?: false,
                historySearch = saved["historySearch"] ?: "",
                historyType = saved["historyType"],
                historyCategoryId = saved["historyCategoryId"],
                transactionDraft = saved.get<ArrayList<String>>("transactionDraft")?.let(HorizonDraft::restore),
                subscriptionId = saved["subscription"],
                draft = saved.get<ArrayList<String>>("draft")?.let(HorizonDraft::restore),
                reviewRequired =
                    saved["pending"] ?: false,
            ),
        )
    val uiState = state.asStateFlow()

    init {
        unsavedChanges.track(
            DraftSource.Horizon,
            state.map { it.draft?.dirty == true || it.transactionDraft?.dirty == true },
            viewModelScope,
        )
    }

    fun deleteObligation(id: Long) = mutate(HorizonChange.Delete(HorizonRecord.Deduction, id), closeDraft = true)

    fun consumeMessage() {
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

    fun refresh() {
        refreshOverview()
        refreshRecent()
        if (state.value.historyOpen) refreshHistory()
        refreshPurchases()
        state.value.subscriptionId?.let(::showPayments)
    }

    fun refreshOverview() =
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

    fun refreshPurchases() =
        fetch("purchases", {
            horizon.purchases()
        }, { it.purchases }) { s, v -> s.copy(purchases = v) }

    fun refreshRecent() = fetch("recent", { getTransactions(1, "", null, pageSize = 5) }, { it.recent }) { s, v -> s.copy(recent = v) }

    fun openHistory() {
        saved["historyOpen"] = true
        state.update { it.copy(historyOpen = true) }
        refreshHistory()
    }

    fun closeHistory() {
        jobs["history"]?.cancel()
        saved["historyOpen"] = false
        state.update { it.copy(historyOpen = false, history = HorizonSection()) }
    }

    fun filterHistory(
        search: String,
        type: String?,
        categoryId: Long?,
    ) {
        val current = state.value
        if (!current.historyOpen || current.busy) return
        if (search == current.historySearch && type == current.historyType && categoryId == current.historyCategoryId) return
        saved["historySearch"] = search
        saved["historyType"] = type
        saved["historyCategoryId"] = categoryId
        state.update {
            it.copy(
                historySearch = search,
                historyType = type,
                historyCategoryId = categoryId,
                history = HorizonSection(),
            )
        }
        refreshHistory()
    }

    fun refreshHistory() {
        loadHistory(append = false)
    }

    fun loadMoreHistory() {
        val history = state.value.history
        val page = history.value ?: return
        if (!history.loading && page.page < page.totalPages) loadHistory(append = true)
    }

    private var retryHistoryAppend = false

    fun retryHistory() = loadHistory(retryHistoryAppend)

    private fun loadHistory(append: Boolean) {
        val current = state.value
        if (!current.historyOpen) return
        retryHistoryAppend = append
        val previous = current.history.value
        val first = if (append && previous != null) previous.page + 1 else 1
        val last = if (append) first else previous?.page ?: 1
        fetch(
            "history",
            {
                val items = if (append) previous?.items.orEmpty().toMutableList() else mutableListOf()
                for (number in first..last) {
                    when (val result = getTransactions(number, current.historySearch, current.historyType, current.historyCategoryId)) {
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
            { it.history },
        ) { s, v -> s.copy(history = v) }
    }

    fun refreshTransactions(page: Int = saved["page"] ?: 1) {
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

    fun filter(
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

    fun tab(tab: HorizonTab) {
        saved["tab"] = tab.name
        state.update { it.copy(tab = tab) }
    }

    fun showPayments(id: Long) {
        saved["subscription"] = id
        state.update {
            it.copy(subscriptionId = id, payments = if (it.subscriptionId == id) it.payments else HorizonSection())
        }
        refreshTransactions()
        fetch("payments", { getPayments(id) }, { it.payments }) { s, v -> s.copy(payments = v) }
    }

    fun hidePayments() {
        saved["subscription"] = null
        jobs["payments"]?.cancel()
        jobs["transactions"]?.cancel()
        state.update { it.copy(subscriptionId = null, payments = HorizonSection(loading = false)) }
    }

    fun edit(draft: HorizonDraft) {
        if (!state.value.busy &&
            !state.value.reviewRequired
        ) {
            saved["draft"] = draft.saved()
            state.update { it.copy(draft = draft, formError = null, message = null) }
        }
    }

    fun update(draft: HorizonDraft) {
        if (!state.value.busy) editStored(draft.copy(dirty = true))
    }

    private fun editStored(draft: HorizonDraft?) {
        saved["draft"] = draft?.saved()
        state.update { it.copy(draft = draft, formError = null) }
    }

    fun discard() {
        if (!state.value.busy) closeForm()
    }

    fun addTransactionCategory() {
        val current = state.value
        val draft = current.draft ?: return
        if (draft.kind != HorizonForm.Transaction || draft.id != null || current.busy || current.reviewRequired) return
        saved["transactionDraft"] = draft.saved()
        state.update { it.copy(transactionDraft = draft) }
        newForm(HorizonForm.Category)
    }

    private fun closeForm() {
        val parent = state.value.transactionDraft
        saved["transactionDraft"] = null
        saved["draft"] = parent?.saved()
        state.update { it.copy(draft = parent, transactionDraft = null, formError = null) }
    }

    fun newForm(kind: HorizonForm) {
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
                        data?.categories?.find { it.name.contains("subscription", ignoreCase = true) }?.id
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

    fun save() {
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

    fun mutate(
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
                            message =
                                uiText(
                                    R.string.pulse_horizon_the_change_could_not_be_confirmed_refresh_and_review,
                                ),
                        )
                    }
            }
            refresh()
        }
    }

    fun acknowledgeReview() {
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
