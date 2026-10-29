package dev.abhishekan.pulse.feature.fitness
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.FitnessDates
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseDatePickerDialog
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
internal fun FitnessCalendar(
    state: FitnessUiState,
    onDate: (LocalDate) -> Unit,
    onMonth: (Long) -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    var choosing by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            val compact = maxWidth < 480.dp
            val inline = maxWidth >= 304.dp && LocalDensity.current.fontScale < 1.3f
            val fullMonth = state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
            val title: @Composable (Modifier) -> Unit = { modifier ->
                PulseText(
                    if (compact && inline) state.month.format(DateTimeFormatter.ofPattern("MMM yyyy", locale)) else fullMonth,
                    modifier.semantics {
                        heading()
                        contentDescription = fullMonth
                        liveRegion = LiveRegionMode.Polite
                    },
                    if (compact && inline) PulseTextStyle.Title3 else PulseTextStyle.Title,
                )
            }
            val controls: @Composable () -> Unit = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CalendarControl(
                        stringResource(R.string.pulse_fitness_previous_month),
                        FitnessSymbol.Previous,
                        !state.busy && state.month > YearMonth.of(0, 1),
                        compact = compact && inline,
                    ) { onMonth(-1) }
                    CalendarControl(
                        stringResource(R.string.pulse_fitness_next_month),
                        FitnessSymbol.Next,
                        !state.busy && state.month < YearMonth.of(9999, 12),
                        compact = compact && inline,
                    ) { onMonth(1) }
                    if (compact && inline) {
                        CompactTodayControl(stringResource(R.string.pulse_fitness_today), !state.busy, onToday)
                    } else {
                        PulseButton(
                            stringResource(R.string.pulse_fitness_today),
                            onToday,
                            enabled = !state.busy,
                            style = PulseButtonStyle.Text,
                        )
                    }
                    CalendarControl(
                        stringResource(R.string.pulse_fitness_choose_date),
                        FitnessSymbol.Calendar,
                        !state.busy,
                        compact = compact && inline,
                    ) { choosing = true }
                }
            }
            if (inline) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
                ) {
                    title(Modifier.weight(1f))
                    controls()
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                    title(Modifier)
                    controls()
                }
            }
        }
        val days = remember(state.month) { FitnessDates.calendar(state.month) }
        val workouts =
            state.overview
                ?.workouts
                .orEmpty()
                .groupingBy { it.date }
                .eachCount()
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val weekdayStyle = if (maxWidth / 7 < 28.dp * LocalDensity.current.fontScale) TextStyle.NARROW else TextStyle.SHORT
            // Seven columns still have 44dp targets at 320dp; avoid Material's 48dp
            // layout minimum expanding neighboring cells into each other on small phones.
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 44.dp) {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny),
                ) {
                    Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
                        DayOfWeek.entries
                            .map {
                                it.getDisplayName(weekdayStyle, locale)
                            }.forEach { day ->
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    PulseText(day, style = PulseTextStyle.Label)
                                }
                            }
                    }
                    days.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth()) {
                            week.forEach { date ->
                                val count = workouts[date] ?: 0
                                val today = date == state.overview?.today
                                val description =
                                    pluralStringResource(
                                        if (today) {
                                            R.plurals.pulse_fitness_today_calendar_workouts
                                        } else {
                                            R.plurals.pulse_fitness_calendar_workouts
                                        },
                                        count,
                                        date.format(
                                            DateTimeFormatter
                                                .ofPattern(
                                                    "EEEE, d MMMM yyyy",
                                                ).withLocale(locale),
                                        ),
                                        count,
                                    )
                                FitnessCalendarDay(
                                    date.dayOfMonth.toString(),
                                    description,
                                    date == state.selected,
                                    today,
                                    count > 0,
                                    { onDate(date) },
                                    Modifier.weight(1f),
                                    enabled =
                                        !state.busy && date.year in 0..9999,
                                    inMonth =
                                        YearMonth.from(date) == state.month,
                                )
                            }
                        }
                    }
                }
            }
        }
        FlowRow(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
        ) {
            CalendarLegend(stringResource(R.string.pulse_fitness_workout_recorded_legend), PulseTheme.colors.success)
            CalendarLegend(stringResource(R.string.pulse_fitness_selected_day_legend), PulseTheme.colors.primary)
            CalendarLegend(stringResource(R.string.pulse_fitness_today), PulseTheme.colors.action, outlined = true)
        }
    }
    if (choosing) {
        PulseDatePickerDialog(state.selected, {
            onDate(it)
            choosing = false
        }, {
            choosing =
                false
        })
    }
}
