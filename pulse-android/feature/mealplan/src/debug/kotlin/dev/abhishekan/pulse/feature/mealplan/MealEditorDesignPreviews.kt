package dev.abhishekan.pulse.feature.mealplan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.MealTime
import dev.abhishekan.pulse.core.ui.PulseTheme
import java.time.LocalTime

@Preview(name = "Add meal · phone", widthDp = 360, heightDp = 800)
@Preview(name = "Add meal · large text", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun AddMealPhonePreview() = AddMealDesignPreview(false)

@Preview(name = "Add meal · dark tablet", widthDp = 900, heightDp = 900)
@Composable
private fun AddMealTabletPreview() = AddMealDesignPreview(true)

@Composable
private fun AddMealDesignPreview(dark: Boolean) {
    val times =
        listOf(
            MealTime(1, "Breakfast", LocalTime.of(7, 0), LocalTime.of(10, 0), true),
            MealTime(2, "Lunch", LocalTime.NOON, LocalTime.of(14, 30), true),
            MealTime(3, "Evening Snacks", LocalTime.of(16, 30), LocalTime.of(18, 30), true),
            MealTime(4, "Dinner", LocalTime.of(19, 30), LocalTime.of(22, 0), true),
            MealTime(5, "After a late workout", LocalTime.of(23, 0), LocalTime.of(1, 0), false),
        )
    PulseTheme(dark) {
        Surface {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                MealEditor(
                    MealDraft(date = "2026-10-18", timeId = 1),
                    times,
                    false,
                    false,
                    null,
                    {},
                    {},
                    {},
                )
            }
        }
    }
}
