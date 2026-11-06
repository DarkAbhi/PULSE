package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import dev.abhishekan.pulse.core.domain.model.HorizonTransactionPage
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseSelectionField
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.currencyText
import dev.abhishekan.pulse.core.ui.pulseDescription
import java.time.format.DateTimeFormatter

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
                currencyText(item.amount).asString(),
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
                ).withZone(UserTimeZone.zone)
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
        PulseButton(
            stringResource(R.string.pulse_horizon_edit),
            edit,
            Modifier.pulseDescription(stringResource(R.string.pulse_horizon_edit_1_s, label)),
            enabled = enabled,
            style = PulseButtonStyle.Text,
        )
        PulseButton(
            stringResource(R.string.pulse_horizon_delete),
            delete,
            Modifier.pulseDescription(stringResource(R.string.pulse_horizon_delete_1_s, label)),
            enabled = enabled,
            style = PulseButtonStyle.Text,
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
        PulseButton(stringResource(R.string.pulse_horizon_apply_search), {
            vm.filter(search, state.type)
        }, enabled = !state.busy, style = PulseButtonStyle.Text)
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
        PulseButton(
            stringResource(R.string.pulse_horizon_previous_page),
            {
                onPage(page.page - 1)
            },
            enabled =
                !busy && page.page > 1,
            style = PulseButtonStyle.Text,
        )
        PulseButton(
            stringResource(R.string.pulse_horizon_next_page),
            {
                onPage(page.page + 1)
            },
            enabled =
                !busy && page.page < page.totalPages,
            style = PulseButtonStyle.Text,
        )
    }
}
