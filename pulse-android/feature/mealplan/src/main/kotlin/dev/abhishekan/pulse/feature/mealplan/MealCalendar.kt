package dev.abhishekan.pulse.feature.mealplan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
    manageTimes: () -> Unit,
    canManageTimes: Boolean,
    timeCount: Int?,
) {
    val colors = PulseTheme.colors
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    val format = DateTimeFormatter.ofPattern("d MMM yyyy")
    val scroll = rememberLazyListState()
    LaunchedEffect(selected) { scroll.animateScrollToItem(selected.dayOfWeek.value - 1) }
    val todayButton: @Composable () -> Unit = {
        PulseButton(
            text = stringResource(R.string.pulse_mealplan_today_label),
            onClick = jumpToToday,
            enabled = enabled,
            variant = PulseButtonVariant.Secondary,
            leadingIcon = { Icon(painterResource(R.drawable.meal_today), null, Modifier.size(20.dp)) },
        )
    }
    val mealTimes: @Composable () -> Unit = {
        Surface(
            onClick = manageTimes,
            enabled = canManageTimes,
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
        ) {
            Row(
                Modifier.defaultMinSize(minHeight = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(painterResource(R.drawable.meal_clock), null, Modifier.size(24.dp))
                Column {
                    PulseText(stringResource(R.string.pulse_mealplan_meal_times), style = PulseTextStyle.Label)
                    timeCount?.let {
                        PulseText(pluralStringResource(R.plurals.pulse_mealplan_slots, it, it), style = PulseTextStyle.Caption)
                    }
                }
                Icon(painterResource(R.drawable.meal_next), null, Modifier.size(16.dp))
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = pulseWindowSize(maxWidth) == PulseWindowSize.Compact
        val dayWidth = if (compact) 48.dp * LocalDensity.current.fontScale.coerceAtLeast(1f) else 80.dp
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
                        FlowRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = if (selected == today) Arrangement.End else Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (selected != today) todayButton()
                            mealTimes()
                        }
                    }
                }
            }
            MealCalendarDateRow(action = if (wide) mealTimes else null) {
                LazyRow(
                    Modifier.widthIn(max = if (compact) 476.dp else 680.dp).fillMaxWidth(),
                    state = scroll,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 12.dp, Alignment.CenterHorizontally),
                ) {
                    items((0L..6L).map(week::plusDays), key = { it.toEpochDay() }) { day ->
                        val active = day == selected
                        val description =
                            if (day ==
                                today
                            ) {
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
                            enabled = enabled,
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
                                    day.format(DateTimeFormatter.ofPattern("EEE")),
                                    style = if (compact) PulseTextStyle.Caption else PulseTextStyle.Body2,
                                )
                                PulseText(
                                    day.dayOfMonth.toString(),
                                    style = if (compact) PulseTextStyle.Title3 else PulseTextStyle.Title,
                                )
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

/** Keeps the week centered and gives the action a separate row whenever its measured width needs it. */
@Composable
private fun MealCalendarDateRow(
    action: (@Composable () -> Unit)?,
    dates: @Composable () -> Unit,
) {
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            dates()
            action?.invoke()
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val calendar = measurables[0].measure(loose)
        val button = measurables.getOrNull(1)?.measure(loose)
        val width = constraints.maxWidth
        val gap = 16.dp.roundToPx()
        // Reserve equal space on both sides so the week stays centered on larger tablets.
        val beside = button != null && calendar.width + 2 * (button.width + gap) <= width
        val stacked = button != null && !beside
        val calendarTop = if (stacked) button.height + gap else 0
        val height = if (beside) maxOf(calendar.height, button.height) else calendarTop + calendar.height
        layout(width, height) {
            calendar.placeRelative(
                (width - calendar.width) / 2,
                if (beside) (height - calendar.height) / 2 else calendarTop,
            )
            button?.placeRelative(width - button.width, if (beside) (height - button.height) / 2 else 0)
        }
    }
}
