package dev.abhishekan.pulse.feature.fitness

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
internal fun FitnessCalendar(state: FitnessUiState, onDate: (LocalDate) -> Unit, onMonth: (Long) -> Unit, onToday: () -> Unit) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), Modifier.semantics { heading(); liveRegion = LiveRegionMode.Polite }, PulseTextStyle.Title)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            PulseTextButton("Previous month", { onMonth(-1) }, enabled = !state.busy && state.month > YearMonth.of(0, 1))
            PulseTextButton("Next month", { onMonth(1) }, enabled = !state.busy && state.month < YearMonth.of(9999, 12))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            PulseTextButton("Today", onToday, enabled = !state.busy)
            PulseTextButton("Choose date", { choosing = true }, enabled = !state.busy)
        }
        val days = remember(state.month) { FitnessDates.calendar(state.month) }
        val workouts = state.overview?.workouts.orEmpty().groupingBy { it.date }.eachCount()
        val minimumWidth = PulseTokens.target * 7 * maxOf(1f, LocalDensity.current.fontScale / 1.5f)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val available = maxWidth
            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                Column(Modifier.horizontalScroll(rememberScrollState()).width(maxOf(available, minimumWidth)),
                    verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny)) {
                    Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { PulseText(day, style = PulseTextStyle.Label) }
                        }
                    }
                    days.chunked(7).forEach { week -> Row(Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            val count = workouts[date] ?: 0
                            val today = date == state.overview?.today
                            val description = "${date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))}, ${if (today) "today, " else ""}$count workout${if (count == 1) "" else "s"} recorded"
                            PulseCalendarDay(date.dayOfMonth.toString(), description, date == state.selected, today, count > 0,
                                { onDate(date) }, Modifier.weight(1f), enabled = !state.busy && date.year in 0..9999, inMonth = YearMonth.from(date) == state.month)
                        }
                    } }
                }
                if (available < minimumWidth) PulseText("Swipe the calendar sideways to see the full week.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        PulseText("Border: today · Filled: selected · Dot: workout recorded", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (choosing) PulseDatePickerDialog(state.selected, { onDate(it); choosing = false }, { choosing = false })
}
