package dev.abhishekan.pulse.feature.mealplan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseDatePickerDialog
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.pulseWindowSize
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val firstMealWeek = LocalDate.of(1, 1, 1)
private val mealWeekCount = ChronoUnit.WEEKS.between(firstMealWeek, LocalDate.of(9999, 12, 31).minusDays(6)).toInt() + 1

@Composable
internal fun MealCalendar(
    week: LocalDate,
    selected: LocalDate,
    today: LocalDate,
    enabled: Boolean,
    previous: () -> Unit,
    next: () -> Unit,
    select: (LocalDate) -> Unit,
    jumpToToday: () -> Unit,
    onPreferences: (() -> Unit)? = null,
) {
    val colors = PulseTheme.colors
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    val format = DateTimeFormatter.ofPattern("d MMM yyyy")
    val weekPage = ChronoUnit.WEEKS.between(firstMealWeek, week).toInt()
    // The selected date already survives recreation. Start on its week even if a swipe was interrupted.
    val pager = remember { PagerState(currentPage = weekPage) { mealWeekCount } }
    val onSelect by rememberUpdatedState(select)
    LaunchedEffect(pager, weekPage, selected, enabled) {
        if (pager.currentPage != weekPage || pager.currentPageOffsetFraction != 0f || pager.isScrollInProgress) {
            pager.scrollToPage(weekPage)
        }
        if (enabled) {
            snapshotFlow { pager.settledPage }.collect { page ->
                if (page != weekPage) onSelect(selected.plusWeeks((page - weekPage).toLong()))
            }
        }
    }
    val todayButton: @Composable () -> Unit = {
        PulseButton(
            text = stringResource(R.string.pulse_mealplan_today_label),
            onClick = jumpToToday,
            enabled = enabled,
            variant = PulseButtonVariant.Secondary,
            leadingIcon = { Icon(painterResource(R.drawable.meal_today), null, Modifier.size(20.dp)) },
        )
    }
    val preferences: (@Composable () -> Unit)? =
        onPreferences?.let { openPreferences ->
            {
                PulseButton(
                    text = stringResource(R.string.meal_setup_preferences),
                    onClick = openPreferences,
                    style = PulseButtonStyle.Text,
                )
            }
        }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = pulseWindowSize(maxWidth) == PulseWindowSize.Compact
        val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        val calendarWidth = minOf(maxWidth, if (compact) 476.dp else 680.dp)
        val daySpacing = if (compact) 4.dp else 12.dp
        val dayWidth = with(LocalDensity.current) {
            val available = (calendarWidth.roundToPx() - 6 * daySpacing.roundToPx()).coerceAtLeast(0)
            minOf(if (compact) 48.dp * fontScale else 80.dp, (available / 7).toDp())
        }
        val weekdayStyle = if (dayWidth < 36.dp * fontScale) TextStyle.NARROW else TextStyle.SHORT
        val dayNumberStyle =
            when {
                dayWidth < 32.dp * fontScale -> PulseTextStyle.Body2
                compact -> PulseTextStyle.Title3
                else -> PulseTextStyle.Title
            }
        val wide = maxWidth / LocalDensity.current.fontScale >= 840.dp
        Column(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = if (wide) colors.background else colors.surface.copy(alpha = 0.6f),
            ) {
                Column(Modifier.padding(if (wide) 0.dp else 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PulseButton(
                            stringResource(R.string.pulse_mealplan_previous_week),
                            previous,
                            Modifier.background(colors.surface, CircleShape).border(1.dp, colors.border, CircleShape),
                            enabled = enabled,
                            style = PulseButtonStyle.Icon,
                        ) {
                            Icon(painterResource(R.drawable.meal_next), null, Modifier.rotate(180f))
                        }
                        Row(
                            Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PulseButton(
                                text =
                                    stringResource(
                                        R.string.pulse_mealplan_week_range,
                                        week.format(DateTimeFormatter.ofPattern("d MMM")),
                                        week.plusDays(6).format(format),
                                    ),
                                onClick = { pickingDate = true },
                                enabled = enabled,
                                style = PulseButtonStyle.Text,
                            )
                            if (wide && selected != today) todayButton()
                        }
                        PulseButton(
                            stringResource(R.string.pulse_mealplan_next_week),
                            next,
                            Modifier.background(colors.surface, CircleShape).border(1.dp, colors.border, CircleShape),
                            enabled = enabled,
                            style = PulseButtonStyle.Icon,
                        ) {
                            Icon(painterResource(R.drawable.meal_next), null)
                        }
                    }
                    if (!wide) {
                        if (selected != today) todayButton()
                        preferences?.invoke()
                    }
                }
            }
            MealCalendarDateRow(
                startAction = if (wide) preferences else null,
            ) {
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.width(calendarWidth).testTag("meal-week-pager"),
                    userScrollEnabled = enabled,
                ) { page ->
                    val pageWeek = firstMealWeek.plusWeeks(page.toLong())
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(daySpacing, Alignment.CenterHorizontally),
                    ) {
                        repeat(7) { offset ->
                            val day = pageWeek.plusDays(offset.toLong())
                            val active = day == selected
                            val description =
                                if (day == today) {
                                    stringResource(R.string.pulse_mealplan_today_date_description, day.format(mealDate))
                                } else {
                                    day.format(mealDate)
                                }
                            Surface(
                                onClick = { select(day) },
                                modifier =
                                    Modifier.width(dayWidth).semantics {
                                        this.selected = active
                                        contentDescription = description
                                    },
                                enabled = enabled && page == weekPage,
                                shape = RoundedCornerShape(if (compact) 12.dp else 20.dp),
                                color = if (active) colors.primary else colors.surface,
                                contentColor = if (active) colors.onPrimary else colors.textPrimary,
                                border = BorderStroke(1.dp, if (active) colors.primary else colors.border),
                            ) {
                                Column(
                                    Modifier.padding(vertical = if (compact) 8.dp else 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    PulseText(
                                        day.dayOfWeek.getDisplayName(weekdayStyle, Locale.getDefault()),
                                        style = if (compact) PulseTextStyle.Caption else PulseTextStyle.Body2,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    PulseText(
                                        day.dayOfMonth.toString(),
                                        style = dayNumberStyle,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (pickingDate && enabled) {
        PulseDatePickerDialog(selected, {
            select(it)
            pickingDate = false
        }, { pickingDate = false })
    }
}

/** Keeps the week centered and moves preferences above it when space is tight. */
@Composable
private fun MealCalendarDateRow(
    startAction: (@Composable () -> Unit)?,
    dates: @Composable () -> Unit,
) {
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            dates()
            startAction?.invoke()
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val calendar = measurables[0].measure(loose)
        val width = constraints.maxWidth
        val gap = 16.dp.roundToPx()
        val actionConstraints = loose.copy(maxWidth = (width - gap).coerceAtLeast(0) / 2)
        val start = if (startAction != null) measurables[1].measure(actionConstraints) else null
        val actionWidth = start?.width ?: 0
        val actionHeight = start?.height ?: 0
        // Reserve equal space on both sides so the week stays centered on larger tablets.
        val beside = actionWidth > 0 && calendar.width + 2 * (actionWidth + gap) <= width
        val calendarTop = if (actionHeight > 0 && !beside) actionHeight + gap else 0
        val height = if (beside) maxOf(calendar.height, actionHeight) else calendarTop + calendar.height
        layout(width, height) {
            calendar.placeRelative(
                (width - calendar.width) / 2,
                if (beside) (height - calendar.height) / 2 else calendarTop,
            )
            val actionRowHeight = if (beside) height else actionHeight
            start?.placeRelative(0, (actionRowHeight - start.height) / 2)
        }
    }
}
