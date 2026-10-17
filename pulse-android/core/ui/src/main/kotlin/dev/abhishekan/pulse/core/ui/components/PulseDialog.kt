package dev.abhishekan.pulse.core.ui.components

import dev.abhishekan.pulse.core.ui.R

import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.core.ui.asString

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PulseDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(onDismiss, confirmButton = { PulseTextButton(confirmLabel, onConfirm) }, modifier = modifier,
        dismissButton = { PulseTextButton(uiText(R.string.pulse_ui_cancel), onDismiss) }, title = { PulseText(title, style = PulseTextStyle.Title) }, text = { PulseText(message, Modifier.verticalScroll(rememberScrollState())) })
}

@Composable
fun PulseDialog(title: UiText, message: UiText, confirmLabel: UiText, onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) = PulseDialog(title.asString(), message.asString(), confirmLabel.asString(), onConfirm, onDismiss, modifier)
