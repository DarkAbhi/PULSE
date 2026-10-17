package dev.abhishekan.pulse.feature.fitness

import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.fitness.R

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
import java.time.DayOfWeek
import java.time.format.TextStyle
import androidx.compose.ui.platform.LocalConfiguration
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
internal fun FitnessCalendar(state: FitnessUiState, onDate: (LocalDate) -> Unit, onMonth: (Long) -> Unit, onToday: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    var choosing by rememberSaveable { mutableStateOf(false) }
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), Modifier.semantics { heading(); liveRegion = LiveRegionMode.Polite }, PulseTextStyle.Title)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            PulseTextButton(uiText(R.string.pulse_fitness_previous_month), { onMonth(-1) }, enabled = !state.busy && state.month > YearMonth.of(0, 1))
            PulseTextButton(uiText(R.string.pulse_fitness_next_month), { onMonth(1) }, enabled = !state.busy && state.month < YearMonth.of(9999, 12))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            PulseTextButton(uiText(R.string.pulse_fitness_today), onToday, enabled = !state.busy)
            PulseTextButton(uiText(R.string.pulse_fitness_choose_date), { choosing = true }, enabled = !state.busy)
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
                        DayOfWeek.entries.map { it.getDisplayName(TextStyle.NARROW, locale) }.forEach { day ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { PulseText(day, style = PulseTextStyle.Label) }
                        }
                    }
                    days.chunked(7).forEach { week -> Row(Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            val count = workouts[date] ?: 0
                            val today = date == state.overview?.today
                            val description = uiPlural(if (today) R.plurals.pulse_fitness_today_calendar_workouts else R.plurals.pulse_fitness_calendar_workouts, count, date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy").withLocale(locale)), count)
                            PulseCalendarDay(date.dayOfMonth.toString(), description, date == state.selected, today, count > 0,
                                { onDate(date) }, Modifier.weight(1f), enabled = !state.busy && date.year in 0..9999, inMonth = YearMonth.from(date) == state.month)
                        }
                    } }
                }
                if (available < minimumWidth) PulseText(uiText(R.string.pulse_fitness_swipe_the_calendar_sideways_to_see_the_full_week), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        PulseText(uiText(R.string.pulse_fitness_border_today_filled_selected_dot_workout_recorded), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (choosing) PulseDatePickerDialog(state.selected, { onDate(it); choosing = false }, { choosing = false })
}
