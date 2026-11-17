package dev.abhishekan.pulse.feature.horizon.presentation.accounts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import dev.abhishekan.pulse.core.domain.model.FinancialAccount
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseSelectionField
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorForm
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorScaffold
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonFormSection

internal val accountSubtypeLabels =
    mapOf(
        "" to R.string.pulse_horizon_account_unspecified,
        "savings" to R.string.pulse_horizon_account_savings,
        "current" to R.string.pulse_horizon_account_current,
    )

internal val cardNetworkLabels =
    mapOf(
        "visa" to R.string.pulse_horizon_card_visa,
        "mastercard" to R.string.pulse_horizon_card_mastercard,
        "american_express" to R.string.pulse_horizon_card_amex,
        "diners_club" to R.string.pulse_horizon_card_diners,
        "rupay" to R.string.pulse_horizon_card_rupay,
    )

@Composable
internal fun AccountEditorScreen(
    type: AccountType,
    state: AccountsState,
    draft: FinancialAccount,
    onAction: (AccountsAction) -> Unit,
    currency: String = "",
) {
    val enabled = !state.busy
    val draftType = AccountType.entries.firstOrNull { it.value == draft.accountType } ?: type
    HorizonEditorScaffold(
        title = stringResource(if (draft.id == null) type.addTitle else type.editTitle),
        backLabel = stringResource(type.backToList),
        busy = state.busy,
    ) { width ->
        HorizonEditorForm(
            width,
            state.busy,
            state.canSave,
            PulseTokens.contentWidth,
            onSave = { onAction(AccountsAction.Save) },
            onCancel = { onAction(AccountsAction.Cancel) },
        ) {
            val request = state.request
            if (request is AccountsState.Request.Failed) {
                PulseText(
                    request.failure.message,
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = PulseTheme.colors.error,
                )
            }
            HorizonFormSection(
                title = R.string.pulse_horizon_account_details,
                hint = null,
                icon = draftType.icon,
            ) {
                PulseTextField(
                    value = draft.displayName,
                    onValueChange = { onAction(AccountsAction.ChangeName(it)) },
                    label =
                        stringResource(
                            if (draftType == AccountType.Bank) R.string.pulse_horizon_account_name else R.string.pulse_horizon_card_name,
                        ),
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    labelAbove = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                )
                PulseTextField(
                    value = draft.institutionName,
                    onValueChange = { onAction(AccountsAction.ChangeInstitution(it)) },
                    label = stringResource(R.string.pulse_horizon_account_institution),
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    labelAbove = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                )
                if (draft.id != null) {
                    PulseSelectionField(
                        label = stringResource(R.string.pulse_horizon_account_type),
                        selected = stringResource(draftType.label),
                        options = AccountType.entries.map { stringResource(it.label) },
                        onSelect = { onAction(AccountsAction.SelectType(AccountType.entries[it])) },
                        enabled = enabled,
                        labelAbove = true,
                    )
                }
                if (draftType == AccountType.Bank) {
                    PulseSelectionField(
                        label = stringResource(R.string.pulse_horizon_account_subtype),
                        selected =
                            stringResource(
                                accountSubtypeLabels[draft.bankAccountSubtype] ?: R.string.pulse_horizon_account_unspecified,
                            ),
                        options = listOf("savings", "current").map { stringResource(accountSubtypeLabels.getValue(it)) },
                        onSelect = { onAction(AccountsAction.SelectSubtype(listOf("savings", "current")[it])) },
                        enabled = enabled,
                        labelAbove = true,
                    )
                }
            }
            HorizonFormSection(
                title = R.string.pulse_horizon_account_reference,
                hint = null,
                icon = R.drawable.pulse_obligation_document,
            ) {
                PulseTextField(
                    value = draft.holderName,
                    onValueChange = { onAction(AccountsAction.ChangeHolderName(it)) },
                    label =
                        stringResource(
                            if (draftType ==
                                AccountType.Bank
                            ) {
                                R.string.pulse_horizon_holder_name
                            } else {
                                R.string.pulse_horizon_cardholder_name
                            },
                        ),
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    labelAbove = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                )
                if (draftType == AccountType.Bank) {
                    PulseTextField(
                        value = draft.holderEmail,
                        onValueChange = { onAction(AccountsAction.ChangeHolderEmail(it)) },
                        label = stringResource(R.string.pulse_horizon_holder_email),
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        labelAbove = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    )
                    PulseTextField(
                        value = draft.accountNumber,
                        onValueChange = { onAction(AccountsAction.ChangeAccountNumber(it)) },
                        label = stringResource(R.string.pulse_horizon_account_number),
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        labelAbove = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                    )
                } else {
                    PulseTextField(
                        value = draft.cardType,
                        onValueChange = { onAction(AccountsAction.ChangeCardType(it)) },
                        label = stringResource(R.string.pulse_horizon_card_type),
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        labelAbove = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    )
                    PulseSelectionField(
                        label = stringResource(R.string.pulse_horizon_card_network),
                        selected = stringResource(cardNetworkLabels[draft.cardNetwork] ?: R.string.pulse_horizon_account_unspecified),
                        options = cardNetworkLabels.values.map { stringResource(it) },
                        onSelect = { onAction(AccountsAction.SelectNetwork(cardNetworkLabels.keys.elementAt(it))) },
                        enabled = enabled,
                        labelAbove = true,
                    )
                    PulseTextField(
                        value = draft.lastFour,
                        onValueChange = { onAction(AccountsAction.ChangeLastFour(it)) },
                        label = stringResource(R.string.pulse_horizon_account_last_four),
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        labelAbove = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    )
                    PulseTextField(
                        value = draft.creditLimit,
                        onValueChange = { onAction(AccountsAction.ChangeCreditLimit(it)) },
                        label = stringResource(R.string.pulse_horizon_credit_limit, currency),
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        labelAbove = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    )
                    PulseTextField(
                        value = draft.statementDay,
                        onValueChange = { onAction(AccountsAction.ChangeStatementDay(it)) },
                        label = stringResource(R.string.pulse_horizon_statement_day),
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        labelAbove = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    )
                }
            }
        }
    }
}
