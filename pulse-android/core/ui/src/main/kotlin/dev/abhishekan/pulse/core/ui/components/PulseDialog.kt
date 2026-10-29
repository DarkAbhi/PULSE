package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.R

@Composable
fun PulseDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = stringResource(R.string.pulse_ui_cancel),
) {
    AlertDialog(
        onDismiss,
        confirmButton = {
            PulseButton(confirmLabel, onConfirm, style = PulseButtonStyle.Text)
        },
        modifier = modifier,
        shape = PulseTheme.shapes.extraLarge,
        containerColor = PulseTheme.colors.surface,
        titleContentColor = PulseTheme.colors.textPrimary,
        textContentColor = PulseTheme.colors.textSecondary,
        tonalElevation = PulseTheme.elevation.none,
        dismissButton = {
            PulseButton(dismissLabel, onDismiss, style = PulseButtonStyle.Text)
        },
        title = {
            PulseText(title, style = PulseTextStyle.Title)
        },
        text = { PulseText(message, Modifier.verticalScroll(rememberScrollState())) },
    )
}
