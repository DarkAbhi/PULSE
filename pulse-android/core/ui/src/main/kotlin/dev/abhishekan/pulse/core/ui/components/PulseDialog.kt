package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PulseDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(onDismiss, confirmButton = { PulseTextButton(confirmLabel, onConfirm) }, modifier = modifier,
        dismissButton = { PulseTextButton("Cancel", onDismiss) }, title = { PulseText(title, style = PulseTextStyle.Title) }, text = { PulseText(message, Modifier.verticalScroll(rememberScrollState())) })
}
