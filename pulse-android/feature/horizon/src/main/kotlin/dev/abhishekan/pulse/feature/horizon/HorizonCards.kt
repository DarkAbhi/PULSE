package dev.abhishekan.pulse.feature.horizon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.HorizonBudget
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import dev.abhishekan.pulse.core.domain.model.HorizonTransactionPage
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseSelectionField
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.label
import dev.abhishekan.pulse.core.ui.pulseDescription
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.horizon.R
import java.math.BigDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun money(
    currency: String,
    amount: BigDecimal,
): UiText {
    val value = amount.setScale(maxOf(2, amount.scale())).toPlainString()
    return if (currency.isBlank()) {
        UiText.Literal(
            value,
        )
    } else {
        uiText(R.string.pulse_horizon_money, currency, value)
    }
}

@Composable internal fun OverviewCard(
    data: HorizonOverview,
    enabled: Boolean,
    configure: () -> Unit,
) {
    PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
        PulseText(
            stringResource(R.string.pulse_horizon_uncommitted_this_month),
            style = PulseTextStyle.Label,
        )
        PulseText(money(data.currency, data.remaining).asString(), style = PulseTextStyle.Metric)
        PulseText(
            if (data.isOvercommitted) {
                stringResource(
                    R.string.pulse_horizon_your_commitments_exceed_the_starting_pool,
                )
            } else {
                stringResource(
                    R.string.pulse_horizon_a_little_perspective_on_what_s_available,
                )
            },
        )
        PulseText(
            stringResource(
                R.string.pulse_horizon_starting_pool_1_s,
                money(data.currency, data.baseAmount).asString(),
            ),
        )
        if (data.baseAmount.signum() ==
            0
        ) {
            PulseText(
                stringResource(
                    R.string.pulse_horizon_set_a_starting_pool_to_make_this_overview_useful,
                ),
            )
        }
        PulseTextButton(
            stringResource(R.string.pulse_horizon_configure_starting_pool),
            configure,
            enabled = enabled,
        )
    }
}

@Composable internal fun CommitmentCard(data: HorizonOverview) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(
            stringResource(R.string.pulse_horizon_monthly_commitments),
            style = PulseTextStyle.Title,
        )
        PulseText(
            stringResource(
                R.string.pulse_horizon_fixed_obligations_1_s,
                money(data.currency, data.deductionsTotal).asString(),
            ),
        )
        PulseText(
            stringResource(
                R.string.pulse_horizon_subscriptions_1_s,
                money(data.currency, data.subscriptionBurn).asString(),
            ),
        )
        PulseText(
            stringResource(
                R.string.pulse_horizon_committed_1_s,
                data.committedPercent.toPlainString(),
            ),
        )
        PulseText(
            stringResource(
                R.string.pulse_horizon_budget_allocation_1_s,
                money(data.currency, data.allocatedTotal).asString(),
            ),
        )
        PulseText(
            stringResource(
                R.string.pulse_horizon_recent_transaction_total_1_s,
                money(data.currency, data.transactionsTotal).asString(),
            ),
        )
        PulseText(
            stringResource(
                R.string.pulse_horizon_totals_and_projections_come_from_pulse_the_summary_s,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable internal fun ProjectionCard(data: HorizonOverview) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(
            stringResource(R.string.pulse_horizon_looking_ahead),
            style = PulseTextStyle.Title,
        )
        if (data.projections.isEmpty()) {
            PulseEmptyState(
                stringResource(R.string.pulse_horizon_no_projections),
                stringResource(
                    R.string.pulse_horizon_projections_appear_when_the_server_provides_them,
                ),
            )
        }
        data.projections.forEach {
            PulseText(it.label, style = PulseTextStyle.Label)
            PulseText(money(data.currency, it.amount).asString(), style = PulseTextStyle.Metric)
        }
        PulseText(
            stringResource(
                R.string.pulse_horizon_projected_cumulative_uncommitted_funds_based_on_the_current_monthly,
            ),
        )
    }
}

@Composable internal fun BudgetCard(
    item: HorizonBudget,
    currency: String,
    enabled: Boolean,
    edit: () -> Unit,
    delete: () -> Unit,
) {
    PulseCard(Modifier.fillMaxWidth().testTag("horizon-record-${item.id}")) {
        PulseText(item.name, style = PulseTextStyle.Title)
        PulseText(money(currency, item.available).asString(), style = PulseTextStyle.Metric)
        PulseText(
            if (item.isOverBudget) {
                stringResource(
                    R.string.pulse_horizon_over_budget,
                )
            } else {
                stringResource(R.string.pulse_horizon_available)
            },
        )
        PulseText(
            stringResource(
                R.string.pulse_horizon_allocated_1_s_used_2_s,
                money(currency, item.allocated).asString(),
                money(currency, item.used).asString(),
            ),
        )
        PulseText(
            stringResource(
                R.string.pulse_horizon_budget_percent_used,
                item.usagePercent.toPlainString(),
            ),
        )
        LinearProgressIndicator(progress = {
            item.usagePercent
                .divide(
                    BigDecimal(100),
                ).coerceIn(BigDecimal.ZERO, BigDecimal.ONE)
                .toFloat()
        }, modifier = Modifier.fillMaxWidth())
        Actions(item.name, enabled, edit, delete)
    }
}

@Composable internal fun TransactionCard(
    item: HorizonTransaction,
    currency: String,
    enabled: Boolean,
    edit: (() -> Unit)? = null,
    delete: (() -> Unit)? = null,
) {
    PulseCard(Modifier.fillMaxWidth().testTag("horizon-record-${item.id}")) {
        PulseText(item.name, style = PulseTextStyle.Title)
        val credit = item.type == "credit"
        PulseText(
            stringResource(
                R.string.pulse_horizon_signed_amount,
                if (credit) "+" else "−",
                money(currency, item.amount).asString(),
            ),
            style = PulseTextStyle.Metric,
        )
        if (currency.isBlank()) {
            PulseText(
                stringResource(R.string.pulse_horizon_currency_unavailable),
                style = PulseTextStyle.Label,
            )
        }
        PulseText(
            if (credit) {
                stringResource(
                    R.string.pulse_horizon_credit_income_refund,
                )
            } else {
                stringResource(R.string.pulse_horizon_debit_spending)
            },
            style = PulseTextStyle.Label,
        )
        PulseText(
            DateTimeFormatter
                .ofPattern(
                    "d MMM yyyy · HH:mm",
                ).withZone(ZoneId.systemDefault())
                .format(item.date),
        )
        PulseText(stringResource(R.string.pulse_horizon_category_1_s, item.categoryName))
        item.budgetName?.let { PulseText(stringResource(R.string.pulse_horizon_budget_1_s, it)) }
        item.subscriptionName?.let {
            PulseText(stringResource(R.string.pulse_horizon_subscription_1_s, it))
        }
        item.notes?.let { PulseText(it) }
        if (edit != null && delete != null) Actions(item.name, enabled, edit, delete)
    }
}

@Composable internal fun Actions(
    label: String,
    enabled: Boolean,
    edit: () -> Unit,
    delete: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        PulseTextButton(
            stringResource(R.string.pulse_horizon_edit),
            edit,
            Modifier.pulseDescription(stringResource(R.string.pulse_horizon_edit_1_s, label)),
            enabled = enabled,
        )
        PulseTextButton(
            stringResource(R.string.pulse_horizon_delete),
            delete,
            Modifier.pulseDescription(stringResource(R.string.pulse_horizon_delete_1_s, label)),
            enabled = enabled,
        )
    }
}

@Composable internal fun TransactionFilters(
    state: HorizonUiState,
    vm: HorizonViewModel,
) {
    var search by rememberSaveable(state.search) { mutableStateOf(state.search) }
    PulseCard(Modifier.fillMaxWidth()) {
        PulseTextField(
            search,
            {
                search = it
            },
            stringResource(
                R.string.pulse_horizon_search_transactions,
            ),
            Modifier.fillMaxWidth(),
            enabled = !state.busy,
        )
        val types = listOf(null, "debit", "credit")
        PulseSelectionField(
            stringResource(R.string.pulse_horizon_filter),
            state.type?.let { horizonOption(it) }
                ?: stringResource(R.string.pulse_horizon_all_types),
            listOf(
                stringResource(R.string.pulse_horizon_all_types),
                stringResource(R.string.pulse_horizon_debit),
                stringResource(R.string.pulse_horizon_credit),
            ),
            {
                vm.filter(search, types[it])
            },
            Modifier.fillMaxWidth(),
            !state.busy,
        )
        PulseTextButton(stringResource(R.string.pulse_horizon_apply_search), {
            vm.filter(search, state.type)
        }, enabled = !state.busy)
    }
}

@Composable internal fun PageControls(
    page: HorizonTransactionPage,
    busy: Boolean,
    onPage: (Int) -> Unit,
) {
    PulseText(
        pluralStringResource(
            R.plurals.pulse_horizon_transaction_page,
            page.total,
            page.page,
            maxOf(1, page.totalPages),
            page.total,
        ),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        PulseTextButton(
            stringResource(R.string.pulse_horizon_previous_page),
            {
                onPage(page.page - 1)
            },
            enabled =
                !busy && page.page > 1,
        )
        PulseTextButton(
            stringResource(R.string.pulse_horizon_next_page),
            {
                onPage(page.page + 1)
            },
            enabled =
                !busy && page.page < page.totalPages,
        )
    }
}

@Composable internal fun SubscriptionPayments(
    state: HorizonUiState,
    vm: HorizonViewModel,
    enabled: Boolean,
    currency: String,
    link: (Long) -> Unit,
) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(
            stringResource(
                R.string.pulse_horizon_payments_1_s,
                state.overview.value
                    ?.subscriptions
                    ?.find { it.id == state.subscriptionId }
                    ?.name
                    ?: stringResource(R.string.pulse_horizon_subscription),
            ),
            style = PulseTextStyle.Title,
        )
        PulseTextButton(stringResource(R.string.pulse_horizon_close_payments), vm::hidePayments)
        SectionState(state.payments, { state.subscriptionId?.let(vm::showPayments) }) { items ->
            if (items.isEmpty()) {
                PulseEmptyState(
                    stringResource(R.string.pulse_horizon_no_linked_payments),
                    stringResource(R.string.pulse_horizon_link_an_existing_unassigned_debit_below),
                )
            }
            items.forEach { TransactionCard(it, currency, false) }
        }
        PulseText(
            stringResource(R.string.pulse_horizon_link_an_existing_payment),
            style = PulseTextStyle.Title,
        )
        PulseText(
            stringResource(R.string.pulse_horizon_only_unlinked_debits_can_be_linked_use_the_search),
        )
        TransactionFilters(state, vm)
        SectionState(state.transactions, { vm.refreshTransactions() }) { page ->
            page.items.filter { it.type == "debit" && it.subscriptionId == null }.forEach { item ->
                PulseTextButton(
                    stringResource(
                        R.string.pulse_horizon_link_1_s_2_s,
                        item.name,
                        money(currency, item.amount).asString(),
                    ),
                    {
                        link(item.id)
                    },
                    enabled = enabled,
                )
            }
            if (page.items.none {
                    it.type == "debit" && it.subscriptionId == null
                }
            ) {
                PulseText(stringResource(R.string.pulse_horizon_no_eligible_payments_on_this_page))
            }
            PageControls(page, state.busy, { vm.refreshTransactions(it) })
        }
    }
}
