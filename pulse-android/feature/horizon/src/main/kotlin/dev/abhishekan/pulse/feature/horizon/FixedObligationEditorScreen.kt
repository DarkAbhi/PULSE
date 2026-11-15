package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenu
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenuItem
import dev.abhishekan.pulse.core.ui.components.PulseText

@Composable
internal fun FixedObligationEditorScreen(
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
    onDelete: (Long) -> Unit,
) {
    HorizonEditorScaffold(
        title = stringResource(draft.topBarTitle()),
        backLabel = backLabel,
        busy = busy,
        actions = {
            draft.id?.let { id ->
                val name = data?.deductions?.find { it.id == id }?.name ?: draft.name
                ObligationDeleteAction(id, name, canSave, onDelete)
            }
        },
    ) { width ->
        HorizonEditorForm(width, busy, canSave, 1000.dp, onSave, onCancel) {
            notice()
            FixedObligationEditor(draft, data, busy, error, onChange)
        }
    }
}

@Composable
private fun ObligationDeleteAction(
    id: Long,
    name: String,
    enabled: Boolean,
    onDelete: (Long) -> Unit,
) {
    var expanded by remember(id) { mutableStateOf(false) }
    var confirming by rememberSaveable(id) { mutableStateOf(false) }
    Box {
        PulseButton(
            stringResource(R.string.pulse_horizon_obligation_more_actions),
            { expanded = true },
            enabled = enabled,
            style = PulseButtonStyle.Icon,
        ) { Icon(painterResource(R.drawable.pulse_horizon_more), null) }
        PulseDropdownMenu(expanded && enabled, { expanded = false }) {
            PulseDropdownMenuItem(
                text = { PulseText(stringResource(R.string.pulse_horizon_obligation_delete), color = PulseTheme.colors.error) },
                onClick = {
                    expanded = false
                    confirming = true
                },
            )
        }
    }
    if (confirming) {
        PulseDialog(
            stringResource(R.string.pulse_horizon_confirm_deletion),
            stringResource(
                R.string.pulse_horizon_delete_1_s_this_removes_the_fixed_obligation_and,
                name,
            ),
            stringResource(R.string.pulse_horizon_delete),
            {
                confirming = false
                if (enabled) onDelete(id)
            },
            { confirming = false },
        )
    }
}
