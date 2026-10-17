package dev.abhishekan.pulse.feature.mealplan

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
        PulseText((if (date == today) "Today · " else "") + date.format(mealDate), Modifier.semantics { heading() }, PulseTextStyle.Title)
        PulseButton("Add meal", onAdd, enabled = enabled)
        if (meals.isEmpty()) PulseEmptyState("A little room to plan", "Add a meal for this day. Simple plans count, too.")
        meals.sortedWith(compareBy<PlannedMeal> { it.start == null }.thenBy { it.start }.thenBy { it.id }).groupBy { it.timeId }.forEach { (_, group) ->
            PulseText(group.first().timeName ?: "Other meals", Modifier.semantics { heading() }, PulseTextStyle.Label, MaterialTheme.colorScheme.primary)
            group.forEach { meal ->
                PulseCard(Modifier.fillMaxWidth(), highlighted = meal.consumed) {
                    PulseText(meal.name, style = PulseTextStyle.Title)
                    val start = meal.start; val end = meal.end
                    if (start != null && end != null) PulseText("${start.format(mealClock)} – ${end.format(mealClock)}${if (end < start) " · overnight" else ""}")
                    PulseCheckbox(meal.consumed, { onToggle(meal) }, "${if (meal.consumed) "✓ Consumed" else "Planned"} · ${meal.name}", enabled = enabled)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                        PulseTextButton("Edit ${meal.name}", { onEdit(meal) }, enabled = enabled)
                        PulseTextButton("Delete ${meal.name}", { onDelete(meal) }, enabled = enabled)
                    }
                }
            }
        }
    }
}

@Composable
internal fun MealTimes(times: List<MealTime>, enabled: Boolean, onAdd: () -> Unit, onDelete: (MealTime) -> Unit) {
    PulseText("Your daily rhythm", Modifier.semantics { heading() }, PulseTextStyle.Display)
    PulseText("Default and custom meal times. Meals can also be planned without a time.")
    PulseButton("Add custom meal time", onAdd, enabled = enabled)
    if (times.isEmpty()) PulseEmptyState("No meal times available", "Create a custom time, or add meals without one.")
    times.forEach { time ->
        PulseCard(Modifier.fillMaxWidth()) {
            PulseText(time.name, style = PulseTextStyle.Title)
            PulseText("${time.start.format(mealClock)} – ${time.end.format(mealClock)}${if (time.end < time.start) " · overnight" else ""}")
            if (time.isDefault) PulseText("Default · protected", style = PulseTextStyle.Label)
            else PulseTextButton("Delete time ${time.name}", { onDelete(time) }, enabled = enabled)
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
                if (day == today) PulseText("Today", style = PulseTextStyle.Label)
                if (day == selected) PulseText("Selected", style = PulseTextStyle.Label)
                val items = meals.filter { it.date == day }
                if (items.isEmpty()) PulseText("No meals yet")
                items.take(3).forEach { meal ->
                    PulseText(meal.name, style = PulseTextStyle.Label)
                    PulseText(if (meal.consumed) "✓ Consumed" else "Planned")
                }
                if (items.size > 3) PulseText("+${items.size - 3} more")
                PulseTextButton("View ${day.format(DateTimeFormatter.ofPattern("EEE"))}", { onSelect(day) }, enabled = enabled)
            }
        }
    }
}
