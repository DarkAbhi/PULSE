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
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenu
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenuItem
import dev.abhishekan.pulse.core.ui.components.PulseText

private enum class NotificationsMenu {
    Closed,
    Options,
    ConfirmClear,
}

@Composable
internal fun NotificationsTopBarActions(
    canClear: Boolean,
    onClear: () -> Unit,
) {
    var menu by rememberSaveable { mutableStateOf(NotificationsMenu.Closed) }

    Box {
        PulseButton(
            stringResource(R.string.pulse_notifications_options),
            { menu = NotificationsMenu.Options },
            style = PulseButtonStyle.Icon,
        ) {
            Icon(painterResource(R.drawable.pulse_notifications_more), contentDescription = null)
        }
        PulseDropdownMenu(
            expanded = menu == NotificationsMenu.Options,
            onDismissRequest = { menu = NotificationsMenu.Closed },
        ) {
            PulseDropdownMenuItem(
                text = { PulseText(stringResource(R.string.pulse_notifications_clear)) },
                enabled = canClear,
                onClick = { menu = NotificationsMenu.ConfirmClear },
            )
        }
    }
    if (menu == NotificationsMenu.ConfirmClear) {
        PulseDialog(
            stringResource(R.string.pulse_notifications_clear_title),
            stringResource(R.string.pulse_notifications_clear_body),
            stringResource(R.string.pulse_notifications_clear),
            {
                menu = NotificationsMenu.Closed
                if (canClear) onClear()
            },
            { menu = NotificationsMenu.Closed },
        )
    }
}
