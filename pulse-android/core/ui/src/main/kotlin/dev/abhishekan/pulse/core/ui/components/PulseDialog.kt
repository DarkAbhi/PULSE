package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.R

@Composable
fun PulseDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismiss,
        confirmButton = {
            PulseTextButton(confirmLabel, onConfirm)
        },
        modifier = modifier,
        dismissButton = {
            PulseTextButton(stringResource(R.string.pulse_ui_cancel), onDismiss)
        },
        title = {
            PulseText(title, style = PulseTextStyle.Title)
        },
        text = { PulseText(message, Modifier.verticalScroll(rememberScrollState())) },
    )
}
