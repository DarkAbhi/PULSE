package dev.abhishekan.pulse.feature.mealplan

import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.mealplan.R

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal val mealDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy")
internal val mealClock: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
internal fun MealAgenda(date: LocalDate, today: LocalDate, meals: List<PlannedMeal>, enabled: Boolean,
    onAdd: () -> Unit, onEdit: (PlannedMeal) -> Unit, onToggle: (PlannedMeal) -> Unit, onDelete: (PlannedMeal) -> Unit,
    modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        PulseText(if (date == today) uiText(R.string.pulse_mealplan_today_date, date.format(mealDate)) else UiText.Literal(date.format(mealDate)), Modifier.semantics { heading() }, PulseTextStyle.Title)
        PulseButton(uiText(R.string.pulse_mealplan_add_meal), onAdd, enabled = enabled)
        if (meals.isEmpty()) PulseEmptyState(uiText(R.string.pulse_mealplan_a_little_room_to_plan), uiText(R.string.pulse_mealplan_add_a_meal_for_this_day_simple_plans_count))
        meals.sortedWith(compareBy<PlannedMeal> { it.start == null }.thenBy { it.start }.thenBy { it.id }).groupBy { it.timeId }.forEach { (_, group) ->
            PulseText(group.first().timeName?.let(UiText::Literal) ?: uiText(R.string.pulse_mealplan_other_meals), Modifier.semantics { heading() }, PulseTextStyle.Label, MaterialTheme.colorScheme.primary)
            group.forEach { meal ->
                PulseCard(Modifier.fillMaxWidth(), highlighted = meal.consumed) {
                    PulseText(meal.name, style = PulseTextStyle.Title)
                    val start = meal.start; val end = meal.end
                    if (start != null && end != null) PulseText(uiText(if (end < start) R.string.pulse_mealplan_overnight_time_range else R.string.pulse_mealplan_time_range, start.format(mealClock), end.format(mealClock)))
                    PulseCheckbox(meal.consumed, { onToggle(meal) }, uiText(R.string.pulse_mealplan_meal_consumption_label , if (meal.consumed) uiText(R.string.pulse_mealplan_consumed) else uiText(R.string.pulse_mealplan_planned), meal.name), enabled = enabled)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                        PulseTextButton(uiText(R.string.pulse_mealplan_edit_1_s , meal.name), { onEdit(meal) }, enabled = enabled)
                        PulseTextButton(uiText(R.string.pulse_mealplan_delete_1_s , meal.name), { onDelete(meal) }, enabled = enabled)
                    }
                }
            }
        }
    }
}

@Composable
internal fun MealTimes(times: List<MealTime>, enabled: Boolean, onAdd: () -> Unit, onDelete: (MealTime) -> Unit) {
    PulseText(uiText(R.string.pulse_mealplan_your_daily_rhythm), Modifier.semantics { heading() }, PulseTextStyle.Display)
    PulseText(uiText(R.string.pulse_mealplan_default_and_custom_meal_times_meals_can_also_be))
    PulseButton(uiText(R.string.pulse_mealplan_add_custom_meal_time), onAdd, enabled = enabled)
    if (times.isEmpty()) PulseEmptyState(uiText(R.string.pulse_mealplan_no_meal_times_available), uiText(R.string.pulse_mealplan_create_a_custom_time_or_add_meals_without_one))
    times.forEach { time ->
        PulseCard(Modifier.fillMaxWidth()) {
            PulseText(time.name, style = PulseTextStyle.Title)
            PulseText(uiText(if (time.end < time.start) R.string.pulse_mealplan_overnight_time_range else R.string.pulse_mealplan_time_range, time.start.format(mealClock), time.end.format(mealClock)))
            if (time.isDefault) PulseText(uiText(R.string.pulse_mealplan_default_protected), style = PulseTextStyle.Label)
            else PulseTextButton(uiText(R.string.pulse_mealplan_delete_time_1_s , time.name), { onDelete(time) }, enabled = enabled)
        }
    }
}

@Composable
internal fun MealWeekBoard(days: List<LocalDate>, selected: LocalDate, today: LocalDate, meals: List<PlannedMeal>, enabled: Boolean, onSelect: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
        days.forEach { day ->
            PulseCard(Modifier.weight(1f), highlighted = day == selected) {
                PulseText(day.format(DateTimeFormatter.ofPattern("EEE")), style = PulseTextStyle.Label)
                PulseText(day.dayOfMonth.toString(), style = PulseTextStyle.Metric)
                if (day == today) PulseText(uiText(R.string.pulse_mealplan_today_label), style = PulseTextStyle.Label)
                if (day == selected) PulseText(uiText(R.string.pulse_mealplan_selected), style = PulseTextStyle.Label)
                val items = meals.filter { it.date == day }
                if (items.isEmpty()) PulseText(uiText(R.string.pulse_mealplan_no_meals_yet))
                items.take(3).forEach { meal ->
                    PulseText(meal.name, style = PulseTextStyle.Label)
                    PulseText(if (meal.consumed) uiText(R.string.pulse_mealplan_consumed) else uiText(R.string.pulse_mealplan_planned))
                }
                if (items.size > 3) PulseText(uiPlural(R.plurals.pulse_mealplan_more_meals, items.size - 3, items.size - 3))
                PulseTextButton(uiText(R.string.pulse_mealplan_view_1_s , day.format(DateTimeFormatter.ofPattern("EEE"))), { onSelect(day) }, enabled = enabled)
            }
        }
    }
}
