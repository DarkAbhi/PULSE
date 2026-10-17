package dev.abhishekan.pulse.feature.horizon

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.usecase.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*

class HorizonViewModel(private val load: LoadHorizon, private val getTransactions: GetHorizonTransactions,
    private val getPurchases: GetPlannedPurchases, private val getPayments: GetSubscriptionPayments,
    private val apply: ApplyHorizonChange, private val saved: SavedStateHandle) : ViewModel() {
    private val state = MutableStateFlow(HorizonUiState(
        tab = HorizonTab.valueOf(saved["tab"] ?: "Overview"), search = saved["search"] ?: "", type = saved["type"],
        subscriptionId = saved["subscription"], draft = saved.get<ArrayList<String>>("draft")?.let(HorizonDraft::restore), reviewRequired = saved["pending"] ?: false))
    val uiState = state.asStateFlow()
    private val jobs = mutableMapOf<String, Job>()
    init { refresh() }
    private fun <T> fetch(key: String, block: suspend () -> Outcome<T>, update: (HorizonUiState, HorizonSection<T>) -> HorizonUiState) {
        jobs[key]?.cancel()
        state.update { update(it, HorizonSection()) }
        jobs[key] = viewModelScope.launch {
            val result = when(val value = block()) {
                is Outcome.Success -> HorizonSection(value.value, loading = false)
                is Outcome.Error -> HorizonSection<T>(loading = false, error = value.failure)
            }
            state.update { update(it, result) }
        }
    }
    fun refresh() { refreshOverview(); refreshTransactions(); refreshPurchases(); state.value.subscriptionId?.let(::showPayments) }
    fun refreshOverview() = fetch("overview", { load() }) { s, v ->
        if (v.value != null && s.subscriptionId != null && v.value.subscriptions.none { it.id == s.subscriptionId }) {
            saved["subscription"] = null
            s.copy(overview = v, subscriptionId = null, payments = HorizonSection(loading = false))
        } else s.copy(overview = v)
    }
    fun refreshPurchases() = fetch("purchases", { getPurchases() }) { s, v -> s.copy(purchases = v) }
    fun refreshTransactions(page: Int = saved["page"] ?: 1) {
        saved["page"] = page
        val search = state.value.search; val type = state.value.type
        fetch("transactions", { getTransactions(page, search, type) }) { s, v ->
            v.value?.let { saved["page"] = it.page }
            s.copy(transactions = v)
        }
    }
    fun filter(search: String, type: String?) {
        if (state.value.busy) return
        saved["search"] = search; saved["type"] = type; saved["page"] = 1
        state.update { it.copy(search = search, type = type) }; refreshTransactions(1)
    }
    fun tab(tab: HorizonTab) { saved["tab"] = tab.name; state.update { it.copy(tab = tab) } }
    fun showPayments(id: Long) {
        saved["subscription"] = id; state.update { it.copy(subscriptionId = id) }
        fetch("payments", { getPayments(id) }) { s, v -> s.copy(payments = v) }
    }
    fun hidePayments() { saved["subscription"] = null; jobs["payments"]?.cancel(); state.update { it.copy(subscriptionId = null, payments = HorizonSection(loading = false)) } }
    fun edit(draft: HorizonDraft) { if (!state.value.busy && !state.value.reviewRequired) { saved["draft"] = draft.saved(); state.update { it.copy(draft = draft, formError = null, message = null) } } }
    fun update(draft: HorizonDraft) { if (!state.value.busy) editStored(draft.copy(dirty = true)) }
    private fun editStored(draft: HorizonDraft?) { saved["draft"] = draft?.saved(); state.update { it.copy(draft = draft, formError = null) } }
    fun discard() { if (!state.value.busy) editStored(null) }
    fun newForm(kind: HorizonForm) {
        val data = state.value.overview.value
        val now = ZonedDateTime.now()
        edit(HorizonDraft(kind, currency = data?.currency ?: "₹", amount = if (kind == HorizonForm.Config) data?.baseAmount?.toPlainString().orEmpty() else "",
            date = if (kind == HorizonForm.Transaction) now.toLocalDate().toString() else "", time = now.toLocalTime().withSecond(0).withNano(0).toString()))
    }
    fun save() {
        if (state.value.busy || state.value.reviewRequired) return
        val draft = state.value.draft ?: return
        val change = try { draft.change() } catch (_: IllegalArgumentException) { null } catch (_: DateTimeException) { null }
        if (change == null || !apply.valid(change)) {
            state.update { it.copy(formError = "Check the fields: a name is required; amounts use up to 2 decimal places (maximum 9,999,999,999.99), transactions must be positive, dates/times must be valid and days range from 1 to 31.") }; return
        }
        mutate(change, closeDraft = true)
    }
    fun mutate(change: HorizonChange, closeDraft: Boolean = false) {
        if (state.value.busy || state.value.reviewRequired) return
        saved["pending"] = true
        state.update { it.copy(busy = true, formError = null, message = null) }
        viewModelScope.launch {
            when(val result = apply(change)) {
                is Outcome.Success -> {
                    saved["pending"] = false
                    if (closeDraft) editStored(null)
                    state.update { it.copy(busy = false, message = "Saved. Your horizon is up to date.") }
                }
                is Outcome.Error -> state.update { it.copy(busy = false, reviewRequired = true,
                    message = "The change could not be confirmed. Refresh and review the server records before trying again.") }
            }
            refresh()
        }
    }
    fun acknowledgeReview() {
        if (state.value.reviewRequired && state.value.reviewReady) {
            saved["pending"] = false
            state.update { it.copy(reviewRequired = false, message = "Server records reviewed. Discard a draft if its change already appears in the records.") }
        }
    }
}
