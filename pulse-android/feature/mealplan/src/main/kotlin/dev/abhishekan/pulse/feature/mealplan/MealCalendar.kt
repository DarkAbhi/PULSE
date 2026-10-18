package dev.abhishekan.pulse.feature.mealplan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseIconButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import dev.abhishekan.pulse.core.ui.R as UiR

@Composable
internal fun MealPlannerHeader(actions: @Composable (Boolean) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val tablet = maxWidth / LocalDensity.current.fontScale >= 840.dp
        val showTitle = maxWidth >= 600.dp
        val title: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(UiR.drawable.pulse_meals), null, Modifier.size(32.dp), tint = PulseTheme.colors.primary)
                PulseText(stringResource(R.string.pulse_mealplan_title), style = PulseTextStyle.Title)
            }
        }
        if (tablet) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                title()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions(showTitle) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showTitle) title()
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) { actions(showTitle) }
            }
        }
    }
}

@Composable
internal fun MealCalendar(
    week: LocalDate,
    selected: LocalDate,
    today: LocalDate,
    enabled: Boolean,
    previous: () -> Unit,
    next: () -> Unit,
    select: (LocalDate) -> Unit,
) {
    val colors = PulseTheme.colors
    val format = DateTimeFormatter.ofPattern("d MMM yyyy")
    val scroll = rememberLazyListState()
    LaunchedEffect(selected) { scroll.animateScrollToItem(selected.dayOfWeek.value - 1) }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(
            Modifier.widthIn(max = 680.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PulseIconButton(
                stringResource(R.string.pulse_mealplan_previous_week),
                previous,
                Modifier.background(colors.surface, CircleShape).border(1.dp, colors.border, CircleShape),
                enabled = enabled,
            ) {
                Icon(painterResource(R.drawable.meal_next), null, Modifier.rotate(180f))
            }
            BoxWithConstraints(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val compact = maxWidth / LocalDensity.current.fontScale < 400.dp
                PulseText(
                    stringResource(
                        R.string.pulse_mealplan_week_range,
                        week.format(if (compact) DateTimeFormatter.ofPattern("d MMM") else format),
                        week.plusDays(6).format(format),
                    ),
                    style = if (compact) PulseTextStyle.Body2 else PulseTextStyle.Title,
                )
            }
            PulseIconButton(
                stringResource(R.string.pulse_mealplan_next_week),
                next,
                Modifier.background(colors.surface, CircleShape).border(1.dp, colors.border, CircleShape),
                enabled = enabled,
            ) {
                Icon(painterResource(R.drawable.meal_next), null)
            }
        }
        LazyRow(
            Modifier.widthIn(max = 680.dp).fillMaxWidth(),
            state = scroll,
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
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
                        Modifier.width(80.dp).semantics {
                            this.selected = active
                            contentDescription = description
                        },
                    enabled = enabled,
                    shape = RoundedCornerShape(20.dp),
                    color = if (active) colors.primary else colors.surface,
                    contentColor = if (active) colors.onPrimary else colors.textPrimary,
                    border = BorderStroke(1.dp, if (active) colors.primary else colors.border),
                ) {
                    Column(
                        Modifier.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        PulseText(day.format(DateTimeFormatter.ofPattern("EEE")), style = PulseTextStyle.Body2)
                        PulseText(day.dayOfMonth.toString(), style = PulseTextStyle.Title)
                    }
                }
            }
        }
    }
}
