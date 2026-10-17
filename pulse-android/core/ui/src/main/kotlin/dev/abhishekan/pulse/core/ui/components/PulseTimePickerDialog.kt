package dev.abhishekan.pulse.core.ui.components

import dev.abhishekan.pulse.core.ui.R

import dev.abhishekan.pulse.core.ui.uiText
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.time.LocalTime
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseTimePickerDialog(selected: LocalTime, onSelected: (LocalTime) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val state = rememberTimePickerState(selected.hour, selected.minute, is24Hour = true)
    AlertDialog(onDismiss, confirmButton = { PulseTextButton(uiText(R.string.pulse_ui_choose_time), { onSelected(LocalTime.of(state.hour, state.minute)) }) },
        modifier = modifier, dismissButton = { PulseTextButton(uiText(R.string.pulse_ui_cancel), onDismiss) }, title = { PulseText(uiText(R.string.pulse_ui_time)) },
        text = { TimeInput(state, Modifier.verticalScroll(rememberScrollState())) })
}
