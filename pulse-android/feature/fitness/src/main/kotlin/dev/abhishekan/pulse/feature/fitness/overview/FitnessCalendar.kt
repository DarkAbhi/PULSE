package dev.abhishekan.pulse.feature.fitness.overview

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abhishekan.pulse.core.domain.model.FitnessDates
import dev.abhishekan.pulse.core.domain.model.WorkoutSummary
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCardPadding
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.fitness.R
import kotlinx.coroutines.flow.first
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
    onRecord: () -> Unit,
    workoutsByDate: Map<LocalDate, List<WorkoutSummary>>,
    modifier: Modifier = Modifier,
) {
    val overview = checkNotNull(state.overview)
    val locale = LocalConfiguration.current.locales[0]
    val month = state.month
    val selectedDate = state.selected
    val monthLabel = month.format(DateTimeFormatter.ofPattern("MMMM", locale))
    val yearLabel = month.format(DateTimeFormatter.ofPattern("uuuu", locale))
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale) }
    val fullDate = selectedDate.format(dateFormatter)
    val selectedPage = month.getLong(ChronoField.PROLEPTIC_MONTH).toInt()
    val pager = remember { PagerState(currentPage = selectedPage) { CALENDAR_MONTH_COUNT } }
    val changeMonth by rememberUpdatedState(onMonth)
    LaunchedEffect(pager, selectedPage, state.busy) {
        // The selected month can also change through an arrow or an adjacent-month date.
        if (pager.currentPage != selectedPage || pager.currentPageOffsetFraction != 0f || pager.isScrollInProgress) {
            pager.scrollToPage(selectedPage)
        }
        if (!state.busy) {
            val page = snapshotFlow { pager.settledPage }.first { it != selectedPage }
            changeMonth((page - selectedPage).toLong())
        }
    }
    var confirmLog by rememberSaveable(selectedDate) { mutableStateOf(false) }
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val compact = screenWidth < 640
    val colors = fitnessCalendarColors()
    val spacing = if (compact) 4.dp else 8.dp
    val cellHeight =
        (screenWidth * .05f).coerceIn(44f, 60f).dp *
            LocalDensity.current.fontScale.coerceAtLeast(1f)
    val cellShape = RoundedCornerShape((screenWidth * .015f).coerceIn(12f, 18f).dp)
    val canLog = selectedDate <= overview.today

    if (confirmLog && state.canRecord) {
        PulseDialog(
            title = stringResource(R.string.pulse_fitness_log_workout_title),
            message = stringResource(R.string.pulse_fitness_log_workout_message, fullDate),
            confirmLabel = stringResource(R.string.pulse_fitness_log_workout),
            onConfirm = {
                confirmLog = false
                onRecord()
            },
            onDismiss = { confirmLog = false },
        )
    }

    PulseCard(modifier.fillMaxWidth(), contentPadding = PulseCardPadding.medium) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val inline = maxWidth >= (if (canLog) 304 else 248).dp * LocalDensity.current.fontScale
            val title: @Composable (Modifier) -> Unit = { titleModifier ->
                Column(
                    titleModifier.clearAndSetSemantics {
                        heading()
                        contentDescription = "$monthLabel $yearLabel"
                        liveRegion = LiveRegionMode.Polite
                    },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val titleSize = if (compact) 24.sp else 30.sp
                    PulseText(
                        monthLabel,
                        style =
                            PulseTheme.typography.title1.copy(
                                fontSize = titleSize,
                                lineHeight = titleSize,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-.6).sp,
                            ),
                    )
                    PulseText(yearLabel, style = PulseTextStyle.Label2, color = colors.muted)
                }
            }
            val controls: @Composable () -> Unit = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (canLog) {
                        FitnessLogWorkoutButton(
                            enabled = state.canRecord,
                            onClick = { confirmLog = true },
                        )
                    }
                    CalendarControl(
                        stringResource(R.string.pulse_fitness_previous_month),
                        FitnessSymbol.Previous,
                        !state.busy && selectedPage > 0,
                    ) { onMonth(-1) }
                    CalendarControl(
                        stringResource(R.string.pulse_fitness_next_month),
                        FitnessSymbol.Next,
                        !state.busy && selectedPage < CALENDAR_MONTH_COUNT - 1,
                    ) { onMonth(1) }
                }
            }
            if (inline) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    title(Modifier.weight(1f))
                    controls()
                }
            } else {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    title(Modifier.fillMaxWidth())
                    controls()
                }
            }
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) {
            Row(
                Modifier.fillMaxWidth().clearAndSetSemantics {},
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                DayOfWeek.entries.forEach { day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val weekdayStyle = if (LocalDensity.current.fontScale >= 1.4f) TextStyle.NARROW else TextStyle.SHORT
                        PulseText(day.getDisplayName(weekdayStyle, locale), style = PulseTextStyle.Label2, color = colors.muted)
                    }
                }
            }
            HorizontalPager(
                state = pager,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .animateContentSize(tween(PulseTokens.MOTION_MILLIS, easing = FastOutSlowInEasing)),
                userScrollEnabled = !state.busy,
            ) { page ->
                val pageMonth = YearMonth.of(page / 12, page % 12 + 1)
                val weeks = remember(pageMonth) { FitnessDates.calendar(pageMonth).chunked(7) }
                val active = page == selectedPage
                Column(
                    modifier = if (active) Modifier else Modifier.clearAndSetSemantics { hideFromAccessibility() },
                    verticalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    weeks.forEach { week ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            week.forEach { date ->
                                val count = workoutsByDate[date]?.size ?: 0
                                val label = date.format(dateFormatter)
                                val detail =
                                    if (count == 0) {
                                        stringResource(R.string.pulse_fitness_calendar_no_workouts)
                                    } else {
                                        pluralStringResource(R.plurals.pulse_fitness_calendar_completed_workouts, count, count)
                                    }
                                FitnessCalendarCell(
                                    date = date,
                                    shape = cellShape,
                                    label = "$label, $detail",
                                    isSelected = date == selectedDate,
                                    isToday = date == overview.today,
                                    inMonth = date.year == pageMonth.year && date.month == pageMonth.month,
                                    hasWorkout = count > 0,
                                    enabled = active && !state.busy && date.year in 0..9999,
                                    onClick = { onDate(date) },
                                    modifier = Modifier.weight(1f).height(cellHeight),
                                )
                            }
                        }
                    }
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CalendarLegend(stringResource(R.string.pulse_fitness_today), Modifier.border(2.dp, colors.today, RoundedCornerShape(4.dp)))
            CalendarLegend(stringResource(R.string.pulse_fitness_calendar_selected), Modifier.background(fitnessCalendarGradient))
            CalendarLegend(stringResource(R.string.pulse_fitness_calendar_workout_logged), Modifier.background(colors.logged), dot = true)
        }
    }
}

@Composable
private fun FitnessCalendarCell(
    date: LocalDate,
    shape: RoundedCornerShape,
    label: String,
    isSelected: Boolean,
    isToday: Boolean,
    inMonth: Boolean,
    hasWorkout: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = fitnessCalendarColors()
    val today = stringResource(R.string.pulse_fitness_today)
    Surface(
        onClick = onClick,
        modifier =
            modifier.clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                selected = isSelected
                if (isToday) stateDescription = today
                if (enabled) {
                    onClick {
                        onClick()
                        true
                    }
                } else {
                    disabled()
                }
            },
        enabled = enabled,
        shape = shape,
        color = if (hasWorkout && !isSelected) colors.logged else Color.Transparent,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .then(if (isSelected) Modifier.background(fitnessCalendarGradient) else Modifier)
                .then(if (isToday) Modifier.border(2.dp, colors.today, shape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            PulseText(
                date.dayOfMonth.toString(),
                style = PulseTheme.typography.label1.copy(fontWeight = if (inMonth) FontWeight.Bold else FontWeight.Medium),
                color =
                    when {
                        isSelected -> Color.White
                        inMonth -> PulseTheme.colors.textPrimary
                        else -> colors.muted
                    },
            )
            if (hasWorkout) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-4).dp)
                        .size(5.dp)
                        .background(if (isSelected) Color.White else colors.dot, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun CalendarLegend(
    label: String,
    swatch: Modifier,
    dot: Boolean = false,
) {
    val colors = fitnessCalendarColors()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).then(swatch), contentAlignment = Alignment.Center) {
            if (dot) Box(Modifier.size(5.dp).background(colors.dot, CircleShape))
        }
        PulseText(label, style = PulseTextStyle.Caption, color = colors.muted)
    }
}
