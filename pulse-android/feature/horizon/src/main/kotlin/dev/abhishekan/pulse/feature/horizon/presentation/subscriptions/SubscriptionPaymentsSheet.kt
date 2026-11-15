package dev.abhishekan.pulse.feature.horizon.presentation.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import dev.abhishekan.pulse.core.domain.model.HorizonTransactionPage
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseBottomSheet
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.currencyText
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pulseDescription
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonSection
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.LedgerRow
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.PageControls
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.TransactionFilters
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.ledgerMonths
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@Composable
internal fun SubscriptionPaymentsSheet(
    subscriptionName: String?,
    payments: HorizonSection<List<HorizonTransaction>>,
    transactions: HorizonSection<HorizonTransactionPage>,
    search: String,
    type: String?,
    busy: Boolean,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onRetryPayments: () -> Unit,
    onRetryTransactions: () -> Unit,
    onFilter: (String, String?) -> Unit,
    onPage: (Int) -> Unit,
    link: (Long) -> Unit,
) {
    val colors = PulseTheme.colors
    PulseBottomSheet(
        title = stringResource(R.string.pulse_horizon_subscription_payment_history),
        fullScreen = true,
        onDismissRequest = onDismiss,
    ) {
        Column(Modifier.fillMaxWidth().testTag("subscription-payments-sheet")) {
            PulseText(
                subscriptionName ?: stringResource(R.string.pulse_horizon_subscription),
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = PulseTextStyle.Body2,
                color = colors.textSecondary,
            )
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (payments.loading && payments.value == null) {
                    PulseCenteredLoadingState(
                        stringResource(R.string.pulse_horizon_loading_your_horizon),
                        Modifier.fillMaxWidth(),
                    )
                }
                payments.error?.let { PulseErrorState(it.message, onRetryPayments) }
                payments.value?.let { linkedPayments ->
                    PulseCard(Modifier.fillMaxWidth().testTag("subscription-payments-summary"), highlighted = true) {
                        PulseText(
                            stringResource(R.string.pulse_horizon_subscription_total_paid),
                            style = PulseTextStyle.Body2,
                            color = colors.onPrimarySoft,
                        )
                        PulseText(
                            currencyText(linkedPayments.fold(BigDecimal.ZERO) { total, payment -> total + payment.amount }).asString(),
                            style = PulseTheme.typography.heading2.copy(fontFeatureSettings = "tnum"),
                        )
                        PulseText(
                            pluralStringResource(
                                R.plurals.pulse_horizon_subscription_payment_count,
                                linkedPayments.size,
                                linkedPayments.size,
                            ),
                            style = PulseTextStyle.Body2,
                        )
                    }
                    if (linkedPayments.isEmpty()) {
                        PulseEmptyState(
                            stringResource(R.string.pulse_horizon_no_linked_payments),
                            stringResource(R.string.pulse_horizon_link_an_existing_unassigned_debit_below),
                        )
                    }
                    ledgerMonths(linkedPayments).forEach { (month, transactions) ->
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
                TransactionFilters(search, type, busy, onFilter)
                if (transactions.loading && transactions.value == null) {
                    PulseCenteredLoadingState(
                        stringResource(R.string.pulse_horizon_loading_your_horizon),
                        Modifier.fillMaxWidth(),
                    )
                }
                transactions.error?.let { PulseErrorState(it.message, onRetryTransactions) }
                transactions.value?.let { page ->
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
                    PageControls(page, busy, onPage)
                }
                if (busy) PulseLoadingState(stringResource(R.string.pulse_horizon_saving_your_change))
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
