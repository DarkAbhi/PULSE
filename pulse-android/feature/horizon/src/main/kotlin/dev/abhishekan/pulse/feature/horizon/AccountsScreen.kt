package dev.abhishekan.pulse.feature.horizon

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
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun AccountsScreen(currency: String, vm: AccountsViewModel = koinViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val draft = state.draft
    BackHandler(draft != null) { if (!state.busy) vm.edit(null) }
    if (draft != null) {
        AccountEditor(draft, state.busy, vm::edit, vm::save)
    } else {
        PulseButton(
            stringResource(R.string.pulse_horizon_add_account),
            { vm.edit(FinancialAccount(currency = currency)) },
            enabled = !state.busy,
        )
        if (state.accounts?.isEmpty() == true) {
            PulseText(stringResource(R.string.pulse_horizon_accounts_empty))
        }
        state.accounts.orEmpty().forEach { account ->
            PulseCard(Modifier.fillMaxWidth()) {
                PulseText(account.displayName, style = PulseTextStyle.Title)
                PulseText(account.institutionName)
                val details = listOfNotNull(
                    stringResource(account.typeLabel()),
                    account.lastFour.takeIf { it.isNotEmpty() }?.let { "•••• $it" },
                    account.currency,
                    if (account.archived) stringResource(R.string.pulse_horizon_account_archived) else null,
                )
                PulseText(details.joinToString(" · "))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                    PulseButton(
                        stringResource(R.string.pulse_horizon_edit_account),
                        { vm.edit(account) },
                        enabled = !state.busy,
                        style = PulseButtonStyle.Text,
                    )
                    PulseButton(
                        stringResource(if (account.archived) R.string.pulse_horizon_restore_account else R.string.pulse_horizon_archive_account),
                        { account.id?.let { vm.archive(it, !account.archived) } },
                        enabled = !state.busy,
                        style = PulseButtonStyle.Text,
                    )
                }
            }
        }
    }
    if (state.busy) {
        PulseLoadingState(stringResource(R.string.pulse_horizon_loading_accounts))
    }
    state.error?.let {
        PulseText(it.message.asString())
        if (draft == null) {
            PulseButton(stringResource(R.string.pulse_horizon_retry_accounts), { vm.refresh() })
        }
    }
}

@Composable
private fun AccountEditor(
    draft: FinancialAccount,
    busy: Boolean,
    edit: (FinancialAccount?) -> Unit,
    save: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
        PulseText(
            stringResource(if (draft.id == null) R.string.pulse_horizon_add_account else R.string.pulse_horizon_edit_account),
            style = PulseTextStyle.Title,
        )
        PulseTextField(
            value = draft.displayName,
            onValueChange = { if (it.length <= 100) edit(draft.copy(displayName = it)) },
            label = stringResource(R.string.pulse_horizon_account_name),
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        PulseTextField(
            value = draft.institutionName,
            onValueChange = { if (it.length <= 150) edit(draft.copy(institutionName = it)) },
            label = stringResource(R.string.pulse_horizon_account_institution),
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            for (type in listOf("bank_account", "credit_card")) {
                PulseButton(
                    stringResource(draft.copy(accountType = type).typeLabel()),
                    { edit(draft.copy(accountType = type, bankAccountSubtype = "")) },
                    enabled = !busy,
                    style = if (draft.accountType == type) PulseButtonStyle.Standard else PulseButtonStyle.Text,
                )
            }
        }
        if (draft.accountType == "bank_account") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                val subtypes = listOf(
                    "" to R.string.pulse_horizon_account_unspecified,
                    "savings" to R.string.pulse_horizon_account_savings,
                    "current" to R.string.pulse_horizon_account_current,
                    "other" to R.string.pulse_horizon_account_other,
                )
                for ((value, label) in subtypes) {
                    PulseButton(
                        stringResource(label), { edit(draft.copy(bankAccountSubtype = value)) },
                        enabled = !busy,
                        style = if (draft.bankAccountSubtype == value) PulseButtonStyle.Standard else PulseButtonStyle.Text,
                    )
                }
            }
        }
        PulseText(stringResource(R.string.pulse_horizon_account_currency, draft.currency))
        PulseTextField(
            value = draft.countryCode,
            onValueChange = { if (it.length <= 2 && it.all { ch -> ch.uppercaseChar() in 'A'..'Z' }) edit(draft.copy(countryCode = it.uppercase())) },
            label = stringResource(R.string.pulse_horizon_account_country),
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        PulseTextField(
            value = draft.lastFour,
            onValueChange = { if (it.length <= 4 && it.all { ch -> ch in '0'..'9' }) edit(draft.copy(lastFour = it)) },
            label = stringResource(R.string.pulse_horizon_account_last_four),
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            PulseButton(stringResource(R.string.pulse_horizon_cancel_account), { edit(null) }, enabled = !busy, style = PulseButtonStyle.Text)
            PulseButton(
                stringResource(R.string.pulse_horizon_save_account),
                save,
                enabled = !busy && draft.displayName.isNotBlank() && draft.institutionName.isNotBlank() &&
                    draft.countryCode.length in listOf(0, 2) && draft.lastFour.length in listOf(0, 4),
            )
        }
    }
}

private fun FinancialAccount.typeLabel(): Int =
    if (accountType == "credit_card") R.string.pulse_horizon_credit_card else R.string.pulse_horizon_bank_account
