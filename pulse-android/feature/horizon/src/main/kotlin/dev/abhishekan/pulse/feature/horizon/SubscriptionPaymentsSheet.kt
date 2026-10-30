package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.currencyText
import dev.abhishekan.pulse.core.ui.pulseDescription
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubscriptionPaymentsSheet(
    state: HorizonUiState,
    vm: HorizonViewModel,
    enabled: Boolean,
    link: (Long) -> Unit,
) {
    val colors = PulseTheme.colors
    val subscription =
        state.overview.value
            ?.subscriptions
            ?.find { it.id == state.subscriptionId }
    ModalBottomSheet(
        onDismissRequest = vm::hidePayments,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.background,
    ) {
        Column(Modifier.fillMaxWidth().testTag("subscription-payments-sheet")) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PulseText(stringResource(R.string.pulse_horizon_subscription_payment_history), style = PulseTextStyle.Heading2)
                    PulseText(
                        subscription?.name ?: stringResource(R.string.pulse_horizon_subscription),
                        style = PulseTextStyle.Body2,
                        color = colors.textSecondary,
                    )
                }
                PulseButton(stringResource(R.string.pulse_horizon_close_payments), vm::hidePayments, style = PulseButtonStyle.Icon) {
                    Icon(painterResource(R.drawable.pulse_purchase_clear), null, Modifier.size(24.dp), tint = colors.textSecondary)
                }
            }
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionState(state.payments, { state.subscriptionId?.let(vm::showPayments) }) { payments ->
                    PulseCard(Modifier.fillMaxWidth().testTag("subscription-payments-summary"), highlighted = true) {
                        PulseText(
                            stringResource(R.string.pulse_horizon_subscription_total_paid),
                            style = PulseTextStyle.Body2,
                            color = colors.onPrimarySoft,
                        )
                        PulseText(
                            currencyText(payments.fold(BigDecimal.ZERO) { total, payment -> total + payment.amount }).asString(),
                            style = PulseTheme.typography.heading2.copy(fontFeatureSettings = "tnum"),
                        )
                        PulseText(
                            pluralStringResource(R.plurals.pulse_horizon_subscription_payment_count, payments.size, payments.size),
                            style = PulseTextStyle.Body2,
                        )
                    }
                    if (payments.isEmpty()) {
                        PulseEmptyState(
                            stringResource(R.string.pulse_horizon_no_linked_payments),
                            stringResource(R.string.pulse_horizon_link_an_existing_unassigned_debit_below),
                        )
                    }
                    ledgerMonths(payments).forEach { (month, transactions) ->
                        key(month) {
                            Surface(shape = PulseTheme.shapes.extraLarge, color = colors.surface) {
                                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    PulseText(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = PulseTextStyle.Title)
                                    PulseText(
                                        pluralStringResource(R.plurals.pulse_horizon_ledger_count, transactions.size, transactions.size),
                                        style = PulseTextStyle.Caption,
                                        color = colors.textSecondary,
                                    )
                                    transactions.forEachIndexed { index, payment ->
                                        key(payment.id) {
                                            PaymentRow(payment)
                                            if (index < transactions.lastIndex) PulseHorizontalDivider(color = colors.divider)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                PulseText(stringResource(R.string.pulse_horizon_link_an_existing_payment), style = PulseTextStyle.Title)
                PulseText(
                    stringResource(R.string.pulse_horizon_only_unlinked_debits_can_be_linked_use_the_search),
                    style = PulseTextStyle.Body2,
                    color = colors.textSecondary,
                )
                TransactionFilters(state, vm)
                SectionState(state.transactions, vm::refreshTransactions) { page ->
                    val eligible = page.items.filter { it.type == "debit" && it.subscriptionId == null }
                    eligible.forEach { payment ->
                        key(payment.id) {
                            PulseCard(Modifier.fillMaxWidth()) {
                                PaymentRow(payment)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    PulseButton(
                                        stringResource(R.string.pulse_horizon_link_payment_action),
                                        { link(payment.id) },
                                        Modifier.pulseDescription(
                                            stringResource(
                                                R.string.pulse_horizon_link_1_s_2_s,
                                                payment.name,
                                                currencyText(payment.amount).asString(),
                                            ),
                                        ),
                                        enabled = enabled,
                                        variant = PulseButtonVariant.Soft,
                                    )
                                }
                            }
                        }
                    }
                    if (eligible.isEmpty()) {
                        PulseText(stringResource(R.string.pulse_horizon_no_eligible_payments_on_this_page), color = colors.textSecondary)
                    }
                    PageControls(page, state.busy, { vm.refreshTransactions(it) })
                }
                if (state.busy) PulseLoadingState(stringResource(R.string.pulse_horizon_saving_your_change))
            }
        }
    }
}

@Composable
private fun PaymentRow(payment: HorizonTransaction) {
    LedgerRow(payment, wide = false)
    payment.budgetName?.let {
        PulseText(
            stringResource(R.string.pulse_horizon_budget_1_s, it),
            style = PulseTextStyle.Caption,
            color = PulseTheme.colors.textSecondary,
        )
    }
    payment.notes?.takeIf { it.isNotBlank() }?.let {
        PulseText(it, Modifier.padding(bottom = 12.dp), style = PulseTextStyle.Body2, color = PulseTheme.colors.textSecondary)
    }
}
