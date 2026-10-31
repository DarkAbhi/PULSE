package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseDatePickerSheet(
    selected: LocalDate,
    select: (LocalDate) -> Unit,
    dismiss: () -> Unit,
    title: String,
    dismissLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = PulseTheme.colors
    val state =
        rememberDatePickerState(
            initialSelectedDateMillis = selected.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange =
                0..9999,
        )
    ModalBottomSheet(
        onDismissRequest = dismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetMaxWidth = 640.dp,
        containerColor = colors.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
                .testTag("pulse-date-sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PulseText(
                title,
                Modifier.padding(horizontal = 24.dp).semantics {
                    heading()
                },
                PulseTextStyle.Title3,
            )
            DatePicker(
                state,
                colors =
                    DatePickerDefaults.colors(
                        containerColor = colors.surface,
                        selectedDayContainerColor = colors.primary,
                        selectedDayContentColor = colors.onPrimary,
                        todayContentColor = colors.action,
                        todayDateBorderColor = colors.action,
                    ),
            )
            PulseButton(
                title,
                {
                    state.selectedDateMillis?.let {
                        select(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                enabled =
                    state.selectedDateMillis != null,
            )
            PulseButton(dismissLabel, dismiss, Modifier.fillMaxWidth(), style = PulseButtonStyle.Text)
        }
    }
}
