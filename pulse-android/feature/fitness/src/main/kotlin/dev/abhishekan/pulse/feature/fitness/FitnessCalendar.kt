package dev.abhishekan.pulse.feature.fitness
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toString
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.domain.model.FitnessDates
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseCalendarDay
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDatePickerDialog
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.fitness.R
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
) {
    val locale = LocalConfiguration.current.locales[0]
    var choosing by rememberSaveable { mutableStateOf(false) }
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(
            state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            Modifier.semantics {
                heading()
                liveRegion =
                    LiveRegionMode.Polite
            },
            PulseTextStyle.Title,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            PulseTextButton(
                stringResource(R.string.pulse_fitness_previous_month),
                {
                    onMonth(-1)
                },
                enabled =
                    !state.busy && state.month > YearMonth.of(0, 1),
            )
            PulseTextButton(
                stringResource(R.string.pulse_fitness_next_month),
                {
                    onMonth(1)
                },
                enabled =
                    !state.busy && state.month < YearMonth.of(9999, 12),
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            PulseTextButton(
                stringResource(R.string.pulse_fitness_today),
                onToday,
                enabled = !state.busy,
            )
            PulseTextButton(stringResource(R.string.pulse_fitness_choose_date), {
                choosing = true
            }, enabled = !state.busy)
        }
        val days = remember(state.month) { FitnessDates.calendar(state.month) }
        val workouts =
            state.overview
                ?.workouts
                .orEmpty()
                .groupingBy { it.date }
                .eachCount()
        val minimumWidth = PulseTokens.target * 7 * maxOf(1f, LocalDensity.current.fontScale / 1.5f)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val available = maxWidth
            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                Column(
                    Modifier
                        .horizontalScroll(
                            rememberScrollState(),
                        ).width(maxOf(available, minimumWidth)),
                    verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny),
                ) {
                    Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
                        DayOfWeek.entries
                            .map {
                                it.getDisplayName(TextStyle.NARROW, locale)
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
                                PulseCalendarDay(
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
                if (available <
                    minimumWidth
                ) {
                    PulseText(
                        stringResource(
                            R.string.pulse_fitness_swipe_the_calendar_sideways_to_see_the_full_week,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        PulseText(
            stringResource(
                R.string.pulse_fitness_border_today_filled_selected_dot_workout_recorded,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
