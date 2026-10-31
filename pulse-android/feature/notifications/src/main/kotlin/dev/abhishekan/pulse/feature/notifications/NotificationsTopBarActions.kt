package dev.abhishekan.pulse.feature.notifications

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenu
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenuItem
import dev.abhishekan.pulse.core.ui.components.PulseText

@Composable
fun NotificationsTopBarActions(viewModel: NotificationsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var expanded by rememberSaveable { mutableStateOf(false) }
    var confirmation by rememberSaveable { mutableStateOf(false) }
    val canClear = state.items.isNotEmpty() && !state.busy && !state.loading && !state.uncertain

    Box {
        PulseButton(stringResource(R.string.pulse_notifications_options), { expanded = true }, style = PulseButtonStyle.Icon) {
            Icon(painterResource(R.drawable.pulse_notifications_more), contentDescription = null)
        }
        PulseDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            PulseDropdownMenuItem(
                text = { PulseText(stringResource(R.string.pulse_notifications_clear)) },
                enabled = canClear,
                onClick = {
                    expanded = false
                    confirmation = true
                },
            )
        }
    }
    if (confirmation) {
        PulseDialog(
            stringResource(R.string.pulse_notifications_clear_title),
            stringResource(R.string.pulse_notifications_clear_body),
            stringResource(R.string.pulse_notifications_clear),
            {
                confirmation = false
                if (canClear) viewModel.clearAll()
            },
            { confirmation = false },
        )
    }
}
