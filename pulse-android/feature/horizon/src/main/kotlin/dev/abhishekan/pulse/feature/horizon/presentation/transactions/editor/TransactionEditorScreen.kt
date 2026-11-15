package dev.abhishekan.pulse.feature.horizon.presentation.transactions.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenu
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenuItem
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonDraft
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorForm
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorScaffold
import dev.abhishekan.pulse.feature.horizon.presentation.editor.topBarTitle

@Composable
internal fun TransactionEditorScreen(
    draft: HorizonDraft,
    busy: Boolean,
    error: UiText?,
    backLabel: String,
    onChange: (HorizonDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    notice: @Composable () -> Unit,
    data: HorizonOverview?,
    canSave: Boolean,
    onAddCategory: () -> Unit,
) {
    HorizonEditorScaffold(
        title = stringResource(draft.topBarTitle()),
        backLabel = backLabel,
        busy = busy,
        actions = {
            if (draft.id == null) TransactionCategoryAction(canSave, onAddCategory)
        },
    ) { width ->
        HorizonEditorForm(width, busy, canSave, PulseTokens.contentWidth, onSave, onCancel) {
            notice()
            TransactionEditor(draft, data, busy, error, onChange)
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
