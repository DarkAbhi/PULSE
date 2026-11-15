package dev.abhishekan.pulse.feature.horizon.presentation.accounts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.FinancialAccount
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.feature.horizon.R
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun AccountsTabScreen(
    currency: String,
    vm: AccountsViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    BackHandler(state.draft != null) { vm.onAction(AccountsAction.Cancel) }
    AccountsTabContent(state, currency, vm::onAction)
}

@Composable
internal fun AccountsTabContent(
    state: AccountsState,
    currency: String,
    onAction: (AccountsAction) -> Unit,
) {
    val draft = state.draft
    if (draft != null) {
        AccountEditor(draft, state.busy, state.canSave, onAction)
    } else {
        PulseButton(
            stringResource(R.string.pulse_horizon_add_account),
            { onAction(AccountsAction.Add(currency)) },
            enabled = !state.busy,
        )
        if (state.accounts?.isEmpty() == true) {
            PulseText(stringResource(R.string.pulse_horizon_accounts_empty))
        }
        state.accounts.orEmpty().forEach { account ->
            PulseCard(Modifier.fillMaxWidth()) {
                PulseText(account.displayName, style = PulseTextStyle.Title)
                PulseText(account.institutionName)
                val details =
                    listOfNotNull(
                        stringResource(accountTypeLabel(account.accountType)),
                        account.lastFour.takeIf { it.isNotEmpty() }?.let { "•••• $it" },
                        account.currency,
                        if (account.archived) stringResource(R.string.pulse_horizon_account_archived) else null,
                    )
                PulseText(details.joinToString(" · "))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                    PulseButton(
                        stringResource(R.string.pulse_horizon_edit_account),
                        { onAction(AccountsAction.Edit(account)) },
                        enabled = !state.busy,
                        style = PulseButtonStyle.Text,
                    )
                    PulseButton(
                        stringResource(
                            if (account.archived) R.string.pulse_horizon_restore_account else R.string.pulse_horizon_archive_account,
                        ),
                        { account.id?.let { onAction(AccountsAction.SetArchived(it, !account.archived)) } },
                        enabled = !state.busy,
                        style = PulseButtonStyle.Text,
                    )
                }
            }
        }
    }
    when (val request = state.request) {
        AccountsState.Request.Idle -> Unit
        AccountsState.Request.Loading -> PulseLoadingState(stringResource(R.string.pulse_horizon_loading_accounts))
        is AccountsState.Request.Failed -> {
            PulseText(request.failure.message.asString())
            if (draft == null) {
                PulseButton(stringResource(R.string.pulse_horizon_retry_accounts), { onAction(AccountsAction.Refresh) })
            }
        }
    }
}

@Composable
private fun AccountEditor(
    draft: FinancialAccount,
    busy: Boolean,
    canSave: Boolean,
    onAction: (AccountsAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
        PulseText(
            stringResource(if (draft.id == null) R.string.pulse_horizon_add_account else R.string.pulse_horizon_edit_account),
            style = PulseTextStyle.Title,
        )
        PulseTextField(
            value = draft.displayName,
            onValueChange = { onAction(AccountsAction.ChangeName(it)) },
            label = stringResource(R.string.pulse_horizon_account_name),
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        PulseTextField(
            value = draft.institutionName,
            onValueChange = { onAction(AccountsAction.ChangeInstitution(it)) },
            label = stringResource(R.string.pulse_horizon_account_institution),
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            for (type in listOf("bank_account", "credit_card")) {
                PulseButton(
                    stringResource(accountTypeLabel(type)),
                    { onAction(AccountsAction.SelectType(type)) },
                    enabled = !busy,
                    style = if (draft.accountType == type) PulseButtonStyle.Standard else PulseButtonStyle.Text,
                )
            }
        }
        if (draft.accountType == "bank_account") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                val subtypes =
                    listOf(
                        "" to R.string.pulse_horizon_account_unspecified,
                        "savings" to R.string.pulse_horizon_account_savings,
                        "current" to R.string.pulse_horizon_account_current,
                        "other" to R.string.pulse_horizon_account_other,
                    )
                for ((value, label) in subtypes) {
                    PulseButton(
                        stringResource(label),
                        { onAction(AccountsAction.SelectSubtype(value)) },
                        enabled = !busy,
                        style = if (draft.bankAccountSubtype == value) PulseButtonStyle.Standard else PulseButtonStyle.Text,
                    )
                }
            }
        }
        PulseText(stringResource(R.string.pulse_horizon_account_currency, draft.currency))
        PulseTextField(
            value = draft.countryCode,
            onValueChange = { onAction(AccountsAction.ChangeCountry(it)) },
            label = stringResource(R.string.pulse_horizon_account_country),
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        PulseTextField(
            value = draft.lastFour,
            onValueChange = { onAction(AccountsAction.ChangeLastFour(it)) },
            label = stringResource(R.string.pulse_horizon_account_last_four),
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            PulseButton(
                stringResource(R.string.pulse_horizon_cancel_account),
                { onAction(AccountsAction.Cancel) },
                enabled = !busy,
                style = PulseButtonStyle.Text,
            )
            PulseButton(
                stringResource(R.string.pulse_horizon_save_account),
                { onAction(AccountsAction.Save) },
                enabled = canSave,
            )
        }
    }
}

private fun accountTypeLabel(accountType: String): Int =
    if (accountType == "credit_card") R.string.pulse_horizon_credit_card else R.string.pulse_horizon_bank_account
