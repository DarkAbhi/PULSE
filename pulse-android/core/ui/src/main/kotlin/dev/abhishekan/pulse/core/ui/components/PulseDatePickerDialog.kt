package dev.abhishekan.pulse.core.ui.components

import dev.abhishekan.pulse.core.ui.R

import dev.abhishekan.pulse.core.ui.uiText

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseDatePickerDialog(selected: LocalDate, onSelected: (LocalDate) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val windowHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val inputMode = windowHeight < 500.dp || density.fontScale > 1.5f
    val state = rememberDatePickerState(initialSelectedDateMillis = selected.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        yearRange = 0..9999, initialDisplayMode = if (inputMode) DisplayMode.Input else DisplayMode.Picker)
    DatePickerDialog(onDismissRequest = onDismiss, modifier = modifier,
        confirmButton = { PulseTextButton(uiText(R.string.pulse_ui_choose_date), {
            state.selectedDateMillis?.let { onSelected(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
        }, enabled = state.selectedDateMillis != null) }, dismissButton = { PulseTextButton(uiText(R.string.pulse_ui_cancel), onDismiss) }) {
        DatePicker(state, Modifier.verticalScroll(rememberScrollState()))
    }
}
