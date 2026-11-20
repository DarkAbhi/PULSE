package dev.abhishekan.pulse.feature.horizon.presentation.transactions.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.FinancialAccount
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenu
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenuItem
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.accounts.AccountsAction
import dev.abhishekan.pulse.feature.horizon.presentation.accounts.AccountsViewModel
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorAction
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorForm
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorScaffold
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorState
import dev.abhishekan.pulse.feature.horizon.presentation.editor.topBarTitle
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun TransactionEditorScreen(
    state: HorizonEditorState,
    backLabel: String,
    onAction: (HorizonEditorAction) -> Unit,
    notice: @Composable () -> Unit,
) {
    val accounts =
        if (state.draft.accountEditable) {
            val vm: AccountsViewModel = koinViewModel()
            val accountState by vm.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(vm) { vm.onAction(AccountsAction.Refresh) }
            accountState.accounts
        } else {
            null
        }
    TransactionEditorScreenContent(state, backLabel, accounts, onAction, notice)
}

@Composable
private fun TransactionEditorScreenContent(
    state: HorizonEditorState,
    backLabel: String,
    accounts: List<FinancialAccount>?,
    onAction: (HorizonEditorAction) -> Unit,
    notice: @Composable () -> Unit,
) {
    val draft = state.draft
    HorizonEditorScaffold(
        title = stringResource(draft.topBarTitle()),
        backLabel = backLabel,
        busy = state.busy,
        actions = {
            if (draft.id == null) {
                TransactionCategoryAction(state.canSave) { onAction(HorizonEditorAction.AddCategory) }
            }
        },
    ) { width ->
        HorizonEditorForm(
            width,
            state.busy,
            state.canSave,
            PulseTokens.contentWidth,
            onSave = { onAction(HorizonEditorAction.Save) },
            onCancel = { onAction(HorizonEditorAction.Cancel) },
        ) {
            notice()
            TransactionEditor(
                draft,
                state.data,
                state.busy,
                state.error,
                onChange = { onAction(HorizonEditorAction.Change(it)) },
                accounts = accounts,
            )
        }
    }
}

@Composable
private fun TransactionCategoryAction(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        PulseButton(
            stringResource(R.string.pulse_horizon_transaction_more_actions),
            { expanded = true },
            enabled = enabled,
            style = PulseButtonStyle.Icon,
        ) { Icon(painterResource(R.drawable.pulse_horizon_more), null) }
        PulseDropdownMenu(expanded && enabled, { expanded = false }) {
            PulseDropdownMenuItem(
                text = { PulseText(stringResource(R.string.pulse_horizon_category_type_action)) },
                onClick = {
                    expanded = false
                    onClick()
                },
            )
        }
    }
}
