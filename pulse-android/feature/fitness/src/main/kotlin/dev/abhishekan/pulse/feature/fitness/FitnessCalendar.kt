package dev.abhishekan.pulse.feature.fitness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.FitnessDates
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCalendarDay
import dev.abhishekan.pulse.core.ui.components.PulseDatePickerDialog
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoField

private const val CALENDAR_MONTH_COUNT = 10_000 * 12

@Composable
internal fun FitnessCalendar(
    state: FitnessUiState,
    onDate: (LocalDate) -> Unit,
    onMonth: (Long) -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val selectedPage = state.month.getLong(ChronoField.PROLEPTIC_MONTH).toInt()
    // The selected date is already restored by the ViewModel.
    val pager = remember { PagerState(currentPage = selectedPage) { CALENDAR_MONTH_COUNT } }
    val scope = rememberCoroutineScope()
    val currentOnMonth by rememberUpdatedState(onMonth)
    LaunchedEffect(pager, state.selected, selectedPage, state.busy) {
        // Today, the date picker and out-of-month dates can select a month directly.
        if (pager.currentPage != selectedPage || pager.currentPageOffsetFraction != 0f || pager.isScrollInProgress) {
            pager.scrollToPage(selectedPage)
        }
        if (!state.busy) {
            val page = snapshotFlow { pager.settledPage }.first { it != selectedPage }
            currentOnMonth((page - selectedPage).toLong())
        }
    }
    var choosing by rememberSaveable { mutableStateOf(false) }
    var weekReset by rememberSaveable { mutableIntStateOf(0) }
    val jumpToToday = {
        weekReset++
        onToday()
    }
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
                FlowRow {
                    CalendarControl(
                        stringResource(R.string.pulse_fitness_previous_month),
                        FitnessSymbol.Previous,
                        !state.busy && selectedPage > 0,
                        compact = compact && inline,
                    ) { scope.launch { pager.animateScrollToPage((pager.targetPage - 1).coerceAtLeast(0)) } }
                    CalendarControl(
                        stringResource(R.string.pulse_fitness_next_month),
                        FitnessSymbol.Next,
                        !state.busy && selectedPage < CALENDAR_MONTH_COUNT - 1,
                        compact = compact && inline,
                    ) { scope.launch { pager.animateScrollToPage((pager.targetPage + 1).coerceAtMost(CALENDAR_MONTH_COUNT - 1)) } }
                    if (compact && inline) {
                        CompactTodayControl(stringResource(R.string.pulse_fitness_today), !state.busy, jumpToToday)
                    } else {
                        PulseButton(
                            stringResource(R.string.pulse_fitness_today),
                            jumpToToday,
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
        val workouts =
            remember(state.overview) {
                state.overview
                    ?.workouts
                    .orEmpty()
                    .groupingBy { it.date }
                    .eachCount()
            }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val grid = with(LocalDensity.current) { constraints.maxWidth >= PulseTokens.target.roundToPx() * 7 }
            val weekdayStyle = if (maxWidth / 7 < 28.dp * LocalDensity.current.fontScale) TextStyle.NARROW else TextStyle.SHORT
            val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale) }
            val adjacentMonthFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, MMMM", locale) }
            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny)) {
                if (grid) {
                    Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
                        DayOfWeek.entries.forEach { day ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                PulseText(day.getDisplayName(weekdayStyle, locale), style = PulseTextStyle.Label)
                            }
                        }
                    }
                }
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.fillMaxWidth(),
                    userScrollEnabled = !state.busy,
                ) { page ->
                    val month = YearMonth.of(page / 12, page % 12 + 1)
                    val weeks = remember(month) { FitnessDates.calendar(month).chunked(7) }
                    val active = page == selectedPage
                    val enabled = active && !state.busy
                    val day: @Composable (LocalDate, Modifier) -> Unit = { date, dayModifier ->
                        val count = workouts[date] ?: 0
                        val today = date == state.overview?.today
                        val fullDate = date.format(dateFormatter)
                        val inMonth = YearMonth.from(date) == month
                        val description =
                            pluralStringResource(
                                if (today) R.plurals.pulse_fitness_today_calendar_workouts else R.plurals.pulse_fitness_calendar_workouts,
                                count,
                                fullDate,
                                count,
                            )
                        PulseCalendarDay(
                            date.dayOfMonth.toString(),
                            description,
                            date == state.selected,
                            today,
                            count > 0,
                            { onDate(date) },
                            dayModifier,
                            enabled = enabled && date.year in 0..9999,
                            inMonth = inMonth,
                            dateLabel =
                                when {
                                    grid -> null
                                    inMonth -> date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
                                    else -> date.format(adjacentMonthFormatter)
                                },
                        )
                    }
                    Column(
                        modifier = if (active) Modifier else Modifier.clearAndSetSemantics { hideFromAccessibility() },
                        verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny),
                    ) {
                        if (grid) {
                            weeks.forEach { week ->
                                Row(Modifier.fillMaxWidth()) {
                                    week.forEach { date -> day(date, Modifier.weight(1f)) }
                                }
                            }
                        } else {
                            val selectedWeek = weeks.indexOfFirst { state.selected in it }.coerceAtLeast(0)
                            var weekIndex by rememberSaveable(page, state.selected, weekReset) { mutableIntStateOf(selectedWeek) }
                            FlowRow(
                                Modifier.fillMaxWidth().padding(horizontal = PulseTokens.small),
                                verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny),
                            ) {
                                CalendarControl(
                                    stringResource(R.string.pulse_fitness_previous_week),
                                    FitnessSymbol.Previous,
                                    enabled && weekIndex > 0,
                                    compact = true,
                                ) { weekIndex-- }
                                PulseText(
                                    stringResource(R.string.pulse_fitness_week_position, weekIndex + 1, weeks.size),
                                    Modifier.align(Alignment.CenterVertically).semantics { liveRegion = LiveRegionMode.Polite },
                                    style = PulseTextStyle.Label,
                                )
                                CalendarControl(
                                    stringResource(R.string.pulse_fitness_next_week),
                                    FitnessSymbol.Next,
                                    enabled && weekIndex < weeks.lastIndex,
                                    compact = true,
                                ) { weekIndex++ }
                            }
                            weeks[weekIndex].forEach { date -> day(date, Modifier.fillMaxWidth()) }
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
            CalendarLegend(
                stringResource(R.string.pulse_fitness_selected_day_legend),
                PulseTheme.colors.primary,
                outline = PulseTheme.colors.action.takeUnless { PulseTheme.isDark },
            )
            CalendarLegend(stringResource(R.string.pulse_fitness_today), Color.Transparent, outline = PulseTheme.colors.action)
        }
    }
    if (choosing) {
        PulseDatePickerDialog(
            state.selected,
            onSelected = {
                weekReset++
                onDate(it)
                choosing = false
            },
            onDismiss = { choosing = false },
            displayedMonth = state.month,
        )
    }
}
