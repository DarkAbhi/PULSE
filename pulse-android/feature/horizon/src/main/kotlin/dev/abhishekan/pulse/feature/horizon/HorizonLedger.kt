package dev.abhishekan.pulse.feature.horizon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import dev.abhishekan.pulse.core.domain.model.HorizonTransactionPage
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.currencyText
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.DateTimeFormatter

internal fun ledgerMonths(items: List<HorizonTransaction>) =
    items.sortedByDescending { it.date }.groupBy { YearMonth.from(it.date.atZone(UserTimeZone.zone)) }

internal fun ledgerSpending(items: List<HorizonTransaction>): BigDecimal =
    items.filter { it.type == "debit" }.fold(BigDecimal.ZERO) { sum, tx -> sum + tx.amount }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HorizonLedger(
    page: HorizonTransactionPage,
    currency: String,
    enabled: Boolean,
    edit: (HorizonTransaction) -> Unit,
    delete: (HorizonTransaction) -> Unit,
    categoryId: Long? = null,
    oldestFirst: Boolean = false,
) {
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val items = page.items.filter { categoryId == null || it.categoryId == categoryId }
    PulseText(stringResource(R.string.pulse_horizon_ledger_scope), style = PulseTextStyle.Caption, color = PulseTheme.colors.textSecondary)
    if (items.isEmpty()) {
        PulseEmptyState(
            stringResource(R.string.pulse_horizon_no_transactions),
            stringResource(R.string.pulse_horizon_try_another_filter_or_record_your_first_transaction),
        )
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 840.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        val ledger: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val months = ledgerMonths(items).entries.toList().let { if (oldestFirst) it.reversed() else it }
                months.forEach { (month, monthItems) ->
                    val transactions = if (oldestFirst) monthItems.reversed() else monthItems
                    key(month) {
                        Surface(shape = RoundedCornerShape(20.dp), color = PulseTheme.colors.surface) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                PulseText(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = PulseTextStyle.Title)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    PulseText(
                                        pluralStringResource(R.plurals.pulse_horizon_ledger_count, transactions.size, transactions.size),
                                        style = PulseTextStyle.Caption,
                                    )
                                    Crossfade(ledgerSpending(transactions), label = "Monthly spending") { amount ->
                                        PulseText(
                                            stringResource(R.string.pulse_horizon_ledger_spent, currencyText(amount).asString()),
                                            style = PulseTextStyle.Caption,
                                        )
                                    }
                                }
                                transactions.forEachIndexed { index, tx ->
                                    key(tx.id) {
                                        var entered by remember { mutableStateOf(false) }
                                        LaunchedEffect(Unit) { entered = true }
                                        AnimatedVisibility(entered, enter = fadeIn(tween(220, minOf(index, 8) * 25))) {
                                            LedgerRow(tx, wide) { selectedId = tx.id }
                                        }
                                        if (index < transactions.lastIndex) HorizontalDivider(color = PulseTheme.colors.divider)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ledger(Modifier.weight(1f))
                LedgerSummary(items, Modifier.width(240.dp))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ledger(Modifier.fillMaxWidth())
                if (items.isNotEmpty()) LedgerSummary(items, Modifier.fillMaxWidth())
            }
        }
    }
    page.items.find { it.id == selectedId }?.let { tx ->
        ModalBottomSheet(onDismissRequest = {
            selectedId = null
        }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = PulseTheme.colors.background) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .padding(bottom = 24.dp),
            ) {
                TransactionCard(tx, currency, false)
                Actions(tx.name, enabled, {
                    selectedId = null
                    edit(tx)
                }, {
                    selectedId = null
                    delete(tx)
                })
            }
        }
    }
}

@Composable
private fun LedgerRow(
    tx: HorizonTransaction,
    wide: Boolean,
    open: () -> Unit,
) {
    val credit = tx.type == "credit"
    val accent = if (credit) PulseTheme.colors.success else PulseTheme.colors.action
    Row(
        Modifier
            .fillMaxWidth()
            .testTag("horizon-record-${tx.id}")
            .clickable(onClick = open)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).background(accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            PulseText(tx.name.take(1).uppercase(), style = PulseTextStyle.Title, color = accent)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PulseText(tx.name, style = PulseTextStyle.Title3)
            PulseText(
                DateTimeFormatter.ofPattern("d MMM · h:mm a").withZone(UserTimeZone.zone).format(tx.date),
                style = PulseTextStyle.Caption,
                color = PulseTheme.colors.textSecondary,
            )
            if (!wide) LedgerBadges(tx)
        }
        if (wide) Box(Modifier.weight(0.8f)) { LedgerBadges(tx) }
        Column(horizontalAlignment = Alignment.End) {
            PulseText(
                stringResource(R.string.pulse_horizon_signed_amount, if (credit) "+" else "−", currencyText(tx.amount).asString()),
                style = PulseTextStyle.Label,
                color = if (credit) PulseTheme.colors.success else PulseTheme.colors.textPrimary,
            )
            PulseText("›", color = PulseTheme.colors.textTertiary)
        }
    }
}

@Composable
private fun LedgerBadges(tx: HorizonTransaction) {
    // Wrap long category names rather than forcing the amount off a narrow screen.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PulseText(
            tx.categoryName,
            Modifier.background(PulseTheme.colors.primarySoft, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
            style = PulseTextStyle.Caption,
            color = PulseTheme.colors.onPrimarySoft,
        )
        PulseText(
            stringResource(if (tx.type == "credit") R.string.pulse_horizon_credit else R.string.pulse_horizon_debit),
            Modifier
                .background(
                    if (tx.type ==
                        "credit"
                    ) {
                        PulseTheme.colors.successSoft
                    } else {
                        PulseTheme.colors.surfaceMuted
                    },
                    RoundedCornerShape(8.dp),
                ).padding(horizontal = 8.dp, vertical = 4.dp),
            style = PulseTextStyle.Caption,
            color = if (tx.type == "credit") PulseTheme.colors.success else PulseTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun LedgerSummary(
    items: List<HorizonTransaction>,
    modifier: Modifier,
) {
    PulseCard(modifier.testTag("horizon-ledger-summary")) {
        PulseText(stringResource(R.string.pulse_horizon_ledger_summary), style = PulseTextStyle.Title)
        PulseText(
            stringResource(R.string.pulse_horizon_ledger_scope),
            style = PulseTextStyle.Caption,
            color = PulseTheme.colors.textSecondary,
        )
        ledgerMonths(items).forEach { (month, transactions) ->
            PulseText(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = PulseTextStyle.Label)
            Crossfade(ledgerSpending(transactions), label = "Spending total") { amount ->
                PulseText(currencyText(amount).asString(), style = PulseTextStyle.Heading3)
            }
        }
        PulseText(stringResource(R.string.pulse_horizon_ledger_debits_only), style = PulseTextStyle.Caption)
    }
}
