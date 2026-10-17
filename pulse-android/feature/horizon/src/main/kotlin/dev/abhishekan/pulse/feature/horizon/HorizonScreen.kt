package dev.abhishekan.pulse.feature.horizon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun HorizonRoute(onDraftChanged: (Boolean) -> Unit, viewModel: HorizonViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.draft?.dirty) { onDraftChanged(state.draft?.dirty == true) }
    HorizonScreen(state, viewModel)
}

@Composable
internal fun HorizonScreen(state: HorizonUiState, vm: HorizonViewModel) {
    val scrollStates = rememberSaveableStateHolder()
    var discard by rememberSaveable { mutableStateOf(false) }
    var records by rememberSaveable { mutableStateOf(false) }
    var deletion by remember { mutableStateOf<Pair<HorizonChange, String>?>(null) }
    val cancel = { if (state.draft?.dirty == true) discard = true else vm.discard() }
    BackHandler(state.draft != null || state.busy || state.subscriptionId != null) {
        when { state.busy -> Unit; state.draft != null -> cancel(); else -> vm.hidePayments() }
    }
    val currency = state.overview.value?.currency.orEmpty()
    PulseAdaptiveLayout(Modifier.fillMaxSize()) { width ->
        Column(Modifier.widthIn(max = PulseTokens.contentWidth).fillMaxWidth().imePadding()) {
            if (state.draft == null || records) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = PulseTokens.medium),
                horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                HorizonTab.entries.forEach { tab -> PulseFilterChip(tab.label, tab == state.tab, { vm.tab(tab) }, enabled = !state.busy) }
            }
            val content: @Composable () -> Unit = {
                val scroll = rememberScrollState()
                LaunchedEffect(state.subscriptionId) { if (state.subscriptionId != null) scroll.scrollTo(0) }
                Column(Modifier.fillMaxWidth().verticalScroll(scroll).padding(if (width == PulseWindowSize.Compact) PulseTokens.medium else PulseTokens.large),
                    verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                    if (state.reviewRequired) PulseCard(Modifier.fillMaxWidth()) {
                        PulseText("Review before another change", style = PulseTextStyle.Title)
                        PulseText("The server may have saved the last change. Check the current records, including any additional transaction pages, to avoid duplicating it.")
                        PulseTextButton("Refresh server records", vm::refresh)
                        if (state.draft != null) PulseTextButton(if (records) "Return to draft" else "View server records", { records = !records })
                        PulseButton("I reviewed the server records", vm::acknowledgeReview, enabled = state.reviewReady && !state.busy)
                    }
                    if (!state.reviewRequired) state.message?.let { PulseText(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (state.draft != null && !records) {
                        if (state.overview.error != null) PulseErrorState(state.overview.error.message, vm::refreshOverview)
                        HorizonEditor(state.draft, state.overview.value, state.busy, state.formError, state.reviewRequired, vm::update, vm::save, cancel)
                    } else {
                        if (state.draft != null && !state.reviewRequired) PulseTextButton("Resume unsaved edits", { records = false })
                        val canMutate = !state.busy && !state.reviewRequired && state.overview.value != null && state.draft == null
                        val edit: (HorizonDraft) -> Unit = { records = false; vm.edit(it) }
                        val create: (HorizonForm) -> Unit = { records = false; vm.newForm(it) }
                        val remove: (HorizonRecord, Long, String) -> Unit = { kind, id, body -> deletion = HorizonChange.Delete(kind, id) to body }
                        when (state.tab) {
                            HorizonTab.Overview -> {
                                PulseText("Your room to move", Modifier.semantics { heading() }, PulseTextStyle.Display)
                                SectionState(state.overview, vm::refreshOverview) { data ->
                                    if (width != PulseWindowSize.Compact && LocalDensity.current.fontScale <= 1.3f) Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                                        Column(Modifier.weight(1.2f)) { OverviewCard(data, canMutate) { create(HorizonForm.Config) } }
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) { CommitmentCard(data); ProjectionCard(data) }
                                    } else { OverviewCard(data, canMutate) { create(HorizonForm.Config) }; CommitmentCard(data); ProjectionCard(data) }
                                }
                            }
                            HorizonTab.Transactions -> {
                                PulseText("The everyday ledger", style = PulseTextStyle.Display)
                                TransactionFilters(state, vm)
                                PulseButton("Add transaction", { create(HorizonForm.Transaction) }, enabled = canMutate)
                                PulseTextButton("Create category", { create(HorizonForm.Category) }, enabled = canMutate)
                                SectionState(state.transactions, { vm.refreshTransactions() }) { page ->
                                    if (page.items.isEmpty()) PulseEmptyState("No transactions", "Try another filter or record your first transaction.")
                                    page.items.forEach { tx -> TransactionCard(tx, currency, canMutate,
                                        { edit(tx.draft(currency)) }, { remove(HorizonRecord.Transaction, tx.id, "Delete “${tx.name}”? This permanently removes the transaction and its budget/subscription association. Budgets and subscriptions stay.") }) }
                                    PageControls(page, state.transactions.loading || state.busy, { vm.refreshTransactions(it) })
                                }
                            }
                            HorizonTab.Budgets -> {
                                PulseText("Give every priority room", style = PulseTextStyle.Display)
                                PulseButton("Add budget", { create(HorizonForm.Budget) }, enabled = canMutate)
                                SectionState(state.overview, vm::refreshOverview) { data ->
                                    if (data.budgets.isEmpty()) PulseEmptyState("No budgets yet", "Create a budget to group related spending and obligations.")
                                    data.budgets.forEach { budget -> BudgetCard(budget, currency, canMutate,
                                        { edit(HorizonDraft(HorizonForm.Budget, budget.id, budget.name, budget.allocated.toPlainString(), currency)) },
                                        { remove(HorizonRecord.Budget, budget.id, "Delete “${budget.name}”? Transactions, obligations and subscriptions remain, with their budget links cleared.") }) }
                                }
                            }
                            HorizonTab.Obligations -> {
                                PulseText("Make room for the essentials", style = PulseTextStyle.Display)
                                PulseButton("Add obligation", { create(HorizonForm.Deduction) }, enabled = canMutate)
                                SectionState(state.overview, vm::refreshOverview) { data ->
                                    if (data.deductions.isEmpty()) PulseEmptyState("No fixed obligations", "Add recurring bills, investments or other commitments.")
                                    data.deductions.forEach { item -> PulseCard(Modifier.fillMaxWidth().testTag("horizon-record-${item.id}")) {
                                        PulseText(item.name, style = PulseTextStyle.Title); PulseText(money(currency, item.amount), style = PulseTextStyle.Metric)
                                        PulseText("${item.category.replaceFirstChar(Char::uppercase)} · ${if (item.active) "Active" else "Inactive"}${item.dueDay?.let { " · Due day $it" }.orEmpty()}")
                                        item.budgetId?.let { id -> PulseText("Budget: ${data.budgets.find { it.id == id }?.name ?: "Unavailable"}") }
                                        Actions(item.name, canMutate, { edit(HorizonDraft(HorizonForm.Deduction, item.id, item.name, item.amount.toPlainString(), currency,
                                            category = item.category, dueDay = item.dueDay?.toString().orEmpty(), active = item.active, budgetId = item.budgetId)) },
                                            { remove(HorizonRecord.Deduction, item.id, "Delete “${item.name}”? This removes the fixed obligation and clears its subscription links. Transactions, subscriptions and budgets remain.") })
                                    } }
                                }
                            }
                            HorizonTab.Subscriptions -> {
                                PulseText("Know what keeps renewing", style = PulseTextStyle.Display)
                                PulseButton("Add subscription", { create(HorizonForm.Subscription) }, enabled = canMutate)
                                if (state.subscriptionId != null) SubscriptionPayments(state, vm, canMutate, currency) { id ->
                                    deletion = HorizonChange.LinkPayment(state.subscriptionId, id) to "Link this debit to the subscription? The transaction remains in your history and gains this subscription association."
                                }
                                SectionState(state.overview, vm::refreshOverview) { data ->
                                    if (data.subscriptions.isEmpty()) PulseEmptyState("No subscriptions", "Add a monthly or yearly subscription to track its commitment.")
                                    data.subscriptions.forEach { item -> PulseCard(Modifier.fillMaxWidth().testTag("horizon-record-${item.id}")) {
                                        PulseText(item.name, style = PulseTextStyle.Title); PulseText(money(currency, item.amount), style = PulseTextStyle.Metric)
                                        PulseText("${item.cycle.replaceFirstChar(Char::uppercase)} · ${item.status.replaceFirstChar(Char::uppercase)}")
                                        PulseText("Monthly equivalent: ${money(currency, item.monthlyAmount)}")
                                        item.nextRenewal?.let { PulseText("Next renewal: $it") }
                                        item.billingDay?.let { PulseText("Billing day: $it") }
                                        PulseText("${item.linkedCount} linked payments · ${money(currency, item.totalSpent)} total spent")
                                        item.notes?.let { PulseText(it) }
                                        Actions(item.name, canMutate, { edit(item.draft(currency)) },
                                            { remove(HorizonRecord.Subscription, item.id, "Delete “${item.name}”? Linked transactions remain with their subscription link cleared. Any linked fixed obligation is kept with its existing active setting.") })
                                        PulseTextButton("View payments for ${item.name}", { vm.showPayments(item.id) }, enabled = !state.busy)
                                    } }
                                }
                            }
                            HorizonTab.Purchases -> {
                                PulseText("A little further ahead", style = PulseTextStyle.Display)
                                PulseText("Next month’s planned purchases")
                                PulseButton("Add planned purchase", { create(HorizonForm.Purchase) }, enabled = canMutate)
                                SectionState(state.purchases, vm::refreshPurchases) { list ->
                                    if (currency.isBlank()) PulseText("Currency unavailable", style = PulseTextStyle.Label)
                                    PulseText("Planned total: ${money(currency, list.total)}", style = PulseTextStyle.Title)
                                    if (list.items.isEmpty()) PulseEmptyState("Nothing planned yet", "Add something you’re setting aside for next month.")
                                    list.items.forEach { item -> PulseCard(Modifier.fillMaxWidth()) {
                                        PulseText(item.name, style = PulseTextStyle.Title); PulseText(money(currency, item.price), style = PulseTextStyle.Metric)
                                        if (currency.isBlank()) PulseText("Currency unavailable", style = PulseTextStyle.Label)
                                        item.url?.let { PulseText(it) }
                                        PulseTextButton("Delete ${item.name}", { remove(HorizonRecord.Purchase, item.id, "Remove “${item.name}” from next month’s planned purchases? This does not delete any transactions.") }, enabled = canMutate)
                                    } }
                                    if (list.items.isNotEmpty()) PulseTextButton("Clear planned purchases", { deletion = HorizonChange.ClearPurchases to "Permanently remove every purchase in next month’s list? Transactions and other months remain unchanged." }, enabled = canMutate)
                                }
                            }
                        }
                    }
                    if (state.busy) PulseLoadingState("Saving your change")
                }
            }
            if (state.draft != null && !records) key(state.draft.kind, state.draft.id) { content() }
            else scrollStates.SaveableStateProvider(state.tab.name) { content() }
        }
    }
    if (discard) PulseDialog("Discard unsaved changes?", "Your edits have not been saved. Leaving this form will discard this device’s draft.", "Discard edits",
        { discard = false; records = false; vm.discard() }, { discard = false })
    deletion?.let { (change, body) -> PulseDialog(if (change is HorizonChange.LinkPayment) "Link payment?" else "Confirm deletion", body,
        if (change is HorizonChange.LinkPayment) "Link payment" else "Delete", { deletion = null; vm.mutate(change) }, { deletion = null }) }
}

@Composable
internal fun <T> SectionState(section: HorizonSection<T>, retry: () -> Unit, content: @Composable (T) -> Unit) {
    when { section.loading -> PulseLoadingState("Loading your horizon")
        section.error != null -> PulseErrorState(section.error.message, retry)
        section.value != null -> content(section.value) }
}
