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
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
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
        "other" to R.string.pulse_horizon_account_other,
    )

@Composable
internal fun AccountEditorScreen(
    type: AccountType,
    state: AccountsState,
    draft: FinancialAccount,
    onAction: (AccountsAction) -> Unit,
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
                        options = accountSubtypeLabels.values.map { stringResource(it) },
                        onSelect = { onAction(AccountsAction.SelectSubtype(accountSubtypeLabels.keys.elementAt(it))) },
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
                PulseText(stringResource(R.string.pulse_horizon_account_currency, draft.currency), style = PulseTextStyle.Label)
                PulseTextField(
                    value = draft.countryCode,
                    onValueChange = { onAction(AccountsAction.ChangeCountry(it)) },
                    label = stringResource(R.string.pulse_horizon_account_country),
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    labelAbove = true,
                    placeholder = { PulseText(stringResource(R.string.pulse_horizon_account_country_hint)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                )
                PulseTextField(
                    value = draft.lastFour,
                    onValueChange = { onAction(AccountsAction.ChangeLastFour(it)) },
                    label = stringResource(R.string.pulse_horizon_account_last_four),
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    labelAbove = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                )
            }
        }
    }
}
