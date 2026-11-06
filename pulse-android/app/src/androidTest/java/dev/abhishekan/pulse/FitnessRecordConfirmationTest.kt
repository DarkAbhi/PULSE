package dev.abhishekan.pulse

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.domain.model.FitnessOverview
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.feature.fitness.FitnessScreen
import dev.abhishekan.pulse.feature.fitness.FitnessUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class FitnessRecordConfirmationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun recordingRequiresConfirmationAndCancelDoesNotRecord() {
        val day = LocalDate.of(2026, 10, 20)
        val state =
            FitnessUiState(
                selected = day,
                month = YearMonth.from(day),
                overview = FitnessOverview(day, emptyList(), emptyList()),
                loading = false,
            )
        var records = 0
        compose.setContent {
            PulseTheme(false) {
                FitnessScreen(state, {}, {}, {}, {}, { records++ }, {}, {})
            }
        }

        compose.onNodeWithText("Add workout").performScrollTo().performClick()
        compose.onNodeWithText("Record workout?").assertIsDisplayed()
        val locale =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext.resources.configuration.locales[0]
        val date = day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale))
        compose.onNodeWithText("Record a workout for $date? You can add exercises and sets afterward.").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, records) }
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Record workout?").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, records) }

        compose.onNodeWithText("Add workout").performScrollTo().performClick()
        compose.onNodeWithText("Record workout").performClick()
        compose.onNodeWithText("Record workout?").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, records) }
    }
}
