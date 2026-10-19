package dev.abhishekan.pulse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import dev.abhishekan.pulse.core.domain.model.FitnessOverview
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.feature.fitness.FitnessScreen
import dev.abhishekan.pulse.feature.fitness.FitnessUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FitnessLoadingLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun onlyInitialLoadingReplacesCalendarAndRefreshKeepsItVisible() {
        val day = LocalDate.of(2026, 10, 20)
        var state by mutableStateOf(FitnessUiState(day, YearMonth.from(day)))
        compose.setContent {
            PulseTheme(false) {
                Box(Modifier.fillMaxSize().testTag("fitness-viewport")) {
                    FitnessScreen(state, {}, {}, {}, {}, {}, {}, {})
                }
            }
        }
        assertLoadingCentered()
        compose.runOnIdle {
            state = state.copy(overview = FitnessOverview(day, emptyList(), emptyList()))
        }
        compose.onNodeWithText("Refreshing your activity").assertDoesNotExist()
        compose.onNodeWithText("SELECTED DAY").assertExists()
        compose.runOnIdle {
            state = state.copy(loading = false, error = dev.abhishekan.pulse.core.domain.model.Failure.Connection)
        }
        compose.onNodeWithText("Refreshing your activity").assertDoesNotExist()
        compose.onNodeWithText("SELECTED DAY").assertExists()
    }

    private fun assertLoadingCentered() {
        val loader = compose.onNodeWithText("Refreshing your activity")
        loader.assertIsDisplayed()
        val bounds = loader.getUnclippedBoundsInRoot()
        val viewport = compose.onNodeWithTag("fitness-viewport").getUnclippedBoundsInRoot()
        assertEquals((viewport.left + viewport.right).value / 2, (bounds.left + bounds.right).value / 2, 1f)
        assertEquals((viewport.top + viewport.bottom).value / 2, (bounds.top + bounds.bottom).value / 2, 1f)
    }
}
