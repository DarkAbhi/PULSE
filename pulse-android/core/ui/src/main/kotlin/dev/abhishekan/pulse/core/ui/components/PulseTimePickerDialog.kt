package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.R
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseTimePickerDialog(
    selected: LocalTime,
    onSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberTimePickerState(selected.hour, selected.minute, is24Hour = true)
    AlertDialog(
        onDismiss,
        confirmButton = {
            PulseTextButton(stringResource(R.string.pulse_ui_choose_time), {
                onSelected(LocalTime.of(state.hour, state.minute))
            })
        },
        modifier = modifier,
        dismissButton = {
            PulseTextButton(stringResource(R.string.pulse_ui_cancel), onDismiss)
        },
        title = { PulseText(stringResource(R.string.pulse_ui_time)) },
        text = { TimeInput(state, Modifier.verticalScroll(rememberScrollState())) },
    )
}
