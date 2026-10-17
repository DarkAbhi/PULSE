package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*
import java.math.BigDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun money(currency: String, amount: BigDecimal): String {
    val value = amount.setScale(maxOf(2, amount.scale())).toPlainString()
    return if (currency.isBlank()) value else "$currency $value"
}
@Composable internal fun OverviewCard(data: HorizonOverview, enabled: Boolean, configure: () -> Unit) {
    PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
        PulseText("UNCOMMITTED THIS MONTH", style = PulseTextStyle.Label)
        PulseText(money(data.currency, data.remaining), style = PulseTextStyle.Metric)
        PulseText(if (data.isOvercommitted) "Your commitments exceed the starting pool." else "A little perspective on what’s available.")
        PulseText("Starting pool: ${money(data.currency, data.baseAmount)}")
        if (data.baseAmount.signum() == 0) PulseText("Set a starting pool to make this overview useful. Zero can also be an intentional setting.")
        PulseTextButton("Configure starting pool", configure, enabled = enabled)
    }
}
@Composable internal fun CommitmentCard(data: HorizonOverview) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText("Monthly commitments", style = PulseTextStyle.Title)
        PulseText("Fixed obligations: ${money(data.currency, data.deductionsTotal)}")
        PulseText("Subscriptions: ${money(data.currency, data.subscriptionBurn)}")
        PulseText("Committed: ${data.committedPercent.toPlainString()}%")
        PulseText("Budget allocation: ${money(data.currency, data.allocatedTotal)}")
        PulseText("Recent transaction total: ${money(data.currency, data.transactionsTotal)}")
        PulseText("Totals and projections come from Pulse. The summary’s transaction total and budget usage use its recent 50 transactions; they are not an all-history balance.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable internal fun ProjectionCard(data: HorizonOverview) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText("Looking ahead", style = PulseTextStyle.Title)
        if (data.projections.isEmpty()) PulseEmptyState("No projections", "Projections appear when the server provides them.")
        data.projections.forEach { PulseText(it.label, style = PulseTextStyle.Label); PulseText(money(data.currency, it.amount), style = PulseTextStyle.Metric) }
        PulseText("Projected cumulative uncommitted funds, based on the current monthly plan.")
    }
}
@Composable internal fun BudgetCard(item: HorizonBudget, currency: String, enabled: Boolean, edit: () -> Unit, delete: () -> Unit) {
    PulseCard(Modifier.fillMaxWidth().testTag("horizon-record-${item.id}")) {
        PulseText(item.name, style = PulseTextStyle.Title)
        PulseText(money(currency, item.available), style = PulseTextStyle.Metric)
        PulseText(if (item.isOverBudget) "Over budget" else "Available")
        PulseText("Allocated: ${money(currency, item.allocated)} · Used: ${money(currency, item.used)}")
        PulseText("${item.usagePercent.toPlainString()}% used")
        LinearProgressIndicator(progress = { item.usagePercent.divide(BigDecimal(100)).coerceIn(BigDecimal.ZERO, BigDecimal.ONE).toFloat() }, modifier = Modifier.fillMaxWidth())
        Actions(item.name, enabled, edit, delete)
    }
}
@Composable internal fun TransactionCard(item: HorizonTransaction, currency: String, enabled: Boolean, edit: (() -> Unit)? = null, delete: (() -> Unit)? = null) {
    PulseCard(Modifier.fillMaxWidth().testTag("horizon-record-${item.id}")) {
        PulseText(item.name, style = PulseTextStyle.Title)
        val credit = item.type == "credit"
        PulseText("${if (credit) "+" else "−"}${money(currency, item.amount)}", style = PulseTextStyle.Metric)
        if (currency.isBlank()) PulseText("Currency unavailable", style = PulseTextStyle.Label)
        PulseText(if (credit) "Credit · Income / refund" else "Debit · Spending", style = PulseTextStyle.Label)
        PulseText(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm").withZone(ZoneId.systemDefault()).format(item.date))
        PulseText("Category: ${item.categoryName}")
        item.budgetName?.let { PulseText("Budget: $it") }; item.subscriptionName?.let { PulseText("Subscription: $it") }; item.notes?.let { PulseText(it) }
        if (edit != null && delete != null) Actions(item.name, enabled, edit, delete)
    }
}
@Composable internal fun Actions(label: String, enabled: Boolean, edit: () -> Unit, delete: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium)) { PulseTextButton("Edit", edit, Modifier.semantics { contentDescription = "Edit $label" }, enabled = enabled); PulseTextButton("Delete", delete, Modifier.semantics { contentDescription = "Delete $label" }, enabled = enabled) }
}
@Composable internal fun TransactionFilters(state: HorizonUiState, vm: HorizonViewModel) {
    var search by rememberSaveable(state.search) { mutableStateOf(state.search) }
    PulseCard(Modifier.fillMaxWidth()) {
        PulseTextField(search, { search = it }, "Search transactions", Modifier.fillMaxWidth(), enabled = !state.busy)
        val types = listOf(null, "debit", "credit")
        PulseSelectionField("Filter", state.type ?: "All types", listOf("All types", "Debit", "Credit"), { vm.filter(search, types[it]) }, Modifier.fillMaxWidth(), !state.busy)
        PulseTextButton("Apply search", { vm.filter(search, state.type) }, enabled = !state.busy)
    }
}
@Composable internal fun PageControls(page: HorizonTransactionPage, busy: Boolean, onPage: (Int) -> Unit) {
    PulseText("Page ${page.page} of ${maxOf(1, page.totalPages)} · ${page.total} transactions")
    Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        PulseTextButton("Previous page", { onPage(page.page - 1) }, enabled = !busy && page.page > 1)
        PulseTextButton("Next page", { onPage(page.page + 1) }, enabled = !busy && page.page < page.totalPages)
    }
}
@Composable internal fun SubscriptionPayments(state: HorizonUiState, vm: HorizonViewModel, enabled: Boolean, currency: String, link: (Long) -> Unit) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText("Payments · ${state.overview.value?.subscriptions?.find { it.id == state.subscriptionId }?.name ?: "Subscription"}", style = PulseTextStyle.Title)
        PulseTextButton("Close payments", vm::hidePayments)
        SectionState(state.payments, { state.subscriptionId?.let(vm::showPayments) }) { items ->
            if (items.isEmpty()) PulseEmptyState("No linked payments", "Link an existing unassigned debit below.")
            items.forEach { TransactionCard(it, currency, false) }
        }
        PulseText("Link an existing payment", style = PulseTextStyle.Title)
        PulseText("Only unlinked debits can be linked. Use the search and page controls to find a payment.")
        TransactionFilters(state, vm)
        SectionState(state.transactions, { vm.refreshTransactions() }) { page ->
            page.items.filter { it.type == "debit" && it.subscriptionId == null }.forEach { item ->
                PulseTextButton("Link ${item.name} · ${money(currency, item.amount)}", { link(item.id) }, enabled = enabled)
            }
            if (page.items.none { it.type == "debit" && it.subscriptionId == null }) PulseText("No eligible payments on this page.")
            PageControls(page, state.busy, { vm.refreshTransactions(it) })
        }
    }
}
