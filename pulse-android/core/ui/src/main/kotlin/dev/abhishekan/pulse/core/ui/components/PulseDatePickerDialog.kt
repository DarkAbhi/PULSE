package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.R
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseDatePickerDialog(
    selected: LocalDate,
    onSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    displayedMonth: YearMonth = YearMonth.from(selected),
) {
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val inputMode = with(density) { windowSize.width.toDp() < 360.dp || windowSize.height.toDp() < 500.dp } || density.fontScale > 1.5f
    val state =
        rememberDatePickerState(
            initialSelectedDateMillis =
                selected
                    .atStartOfDay(
                        ZoneOffset.UTC,
                    ).toInstant()
                    .toEpochMilli(),
            initialDisplayedMonthMillis =
                displayedMonth
                    .atDay(1)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli(),
            yearRange = 0..9999,
            initialDisplayMode = if (inputMode) DisplayMode.Input else DisplayMode.Picker,
        )
    val confirm: @Composable () -> Unit = {
        PulseButton(stringResource(R.string.pulse_ui_choose_date), {
            state.selectedDateMillis?.let {
                onSelected(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
            }
        }, enabled = state.selectedDateMillis != null, style = PulseButtonStyle.Text)
    }
    val cancel: @Composable () -> Unit = {
        PulseButton(stringResource(R.string.pulse_ui_cancel), onDismiss, style = PulseButtonStyle.Text)
    }
    val picker: @Composable () -> Unit = {
        DatePicker(
            state,
            Modifier.verticalScroll(rememberScrollState()),
            showModeToggle = !inputMode,
            colors =
                DatePickerDefaults.colors(
                    containerColor = PulseTheme.colors.surface,
                    selectedDayContainerColor = PulseTheme.colors.primary,
                    selectedDayContentColor = PulseTheme.colors.onPrimary,
                    todayContentColor = PulseTheme.colors.action,
                    todayDateBorderColor = PulseTheme.colors.action,
                ),
        )
    }
    if (inputMode) {
        PulseDialog(onDismiss, confirm, modifier, dismissButton = cancel, text = picker)
    } else {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            modifier = modifier,
            shape = PulseTheme.shapes.extraLarge,
            tonalElevation = PulseTheme.elevation.none,
            colors = DatePickerDefaults.colors(containerColor = PulseTheme.colors.surface),
            confirmButton = confirm,
            dismissButton = cancel,
        ) { picker() }
    }
}
