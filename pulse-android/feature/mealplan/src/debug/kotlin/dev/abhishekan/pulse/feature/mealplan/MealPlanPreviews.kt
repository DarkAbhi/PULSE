package dev.abhishekan.pulse.feature.mealplan
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.MealTime
import dev.abhishekan.pulse.core.domain.model.PlannedMeal
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import java.time.LocalDate
import java.time.LocalTime

private val day = LocalDate.of(2026, 10, 18)
private val fixtures =
    listOf(
        PlannedMeal(1, day, "Omelette", 1, "Breakfast", LocalTime.of(7, 0), LocalTime.of(10, 0), false),
        PlannedMeal(2, day, "Biryani", 2, "Lunch", LocalTime.NOON, LocalTime.of(14, 30), false),
        PlannedMeal(5, day, "Ice cream", 2, "Lunch", LocalTime.NOON, LocalTime.of(14, 30), false),
        PlannedMeal(3, day, "Chickpea", 3, "Evening Snacks", LocalTime.of(16, 30), LocalTime.of(18, 30), false),
        PlannedMeal(4, day, "Pulav", 4, "Dinner", LocalTime.of(19, 30), LocalTime.of(22, 0), false),
    )

@Preview(name = "Phone", widthDp = 360, heightDp = 800)
@Preview(name = "Large text", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun PlannerLight() {
    PlannerPreview(false, fixtures)
}

@Preview(name = "Tablet meals", widthDp = 1280, heightDp = 900)
@Composable
private fun PlannerDark() {
    PlannerPreview(true, fixtures)
}

@Preview(name = "Tablet empty", widthDp = 1280, heightDp = 900)
@Preview(name = "Phone empty", widthDp = 360, heightDp = 800)
@Composable
private fun PlannerEmpty() {
    PlannerPreview(true, emptyList())
}

@Composable
private fun PlannerPreview(
    dark: Boolean,
    meals: List<PlannedMeal>,
) {
    PulseTheme(dark) {
        androidx.compose.material3.Surface {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PulseTokens.cardPadding),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.large),
            ) {
                MealCalendar(day.minusDays(6), day, day, true, {}, {}, {}, {}, {}, true, 4)
                MealAgenda(day, day, meals, true, {}, {}, {}, Modifier.fillMaxWidth())
            }
        }
    }
}

@Preview(name = "Saving form", widthDp = 600, heightDp = 700)
@Composable
private fun SavingMeal() {
    PulseTheme(false) {
        MealEditor(
            MealDraft(
                name = "Soup",
                date = day.toString(),
            ),
            emptyList(),
            true,
            false,
            null,
            {
            },
            {},
            {},
        )
    }
}

@Preview(name = "Error form", widthDp = 360, heightDp = 800)
@Composable
private fun InvalidMealTime() {
    PulseTheme(true) {
        MealEditor(
            MealDraft(
                timeForm = true,
                name = "Snack",
            ),
            emptyList(),
            false,
            false,
            UiText.Literal("Enter a name and valid times."),
            {
            },
            {},
            {},
        )
    }
}

@Preview(name = "Meal times", widthDp = 360, heightDp = 800)
@Preview(name = "Meal times large text", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun MealTimesPreview() {
    PulseTheme(false) { MealTimesPreviewContent() }
}

@Preview(name = "Meal times dark", widthDp = 360, heightDp = 800)
@Composable
private fun MealTimesDarkPreview() {
    PulseTheme(true) { MealTimesPreviewContent() }
}

@Composable
private fun MealTimesPreviewContent() {
    androidx.compose.material3.Surface {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .padding(bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MealTimesHeader({}, true)
                MealTimes(
                    fixtures.map { MealTime(it.timeId!!, it.timeName!!, it.start!!, it.end!!, true) }.distinctBy { it.id } +
                        MealTime(6, "After workout", LocalTime.of(21, 0), LocalTime.of(22, 0), false),
                    true,
                    {},
                )
            }
            AddMealTimeButton({}, Modifier.align(Alignment.BottomEnd).padding(16.dp))
        }
    }
}
