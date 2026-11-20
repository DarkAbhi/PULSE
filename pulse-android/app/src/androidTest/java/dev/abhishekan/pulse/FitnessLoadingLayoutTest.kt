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
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.FitnessOverview
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.feature.fitness.overview.FitnessAction
import dev.abhishekan.pulse.feature.fitness.overview.FitnessScreen
import dev.abhishekan.pulse.feature.fitness.overview.FitnessUiState
import dev.abhishekan.pulse.feature.fitness.shared.FitnessLoadState
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
        var refreshes = 0
        compose.setContent {
            PulseTheme(false) {
                Box(Modifier.fillMaxSize().testTag("fitness-viewport")) {
                    FitnessScreen(
                        state,
                        onAction = {
                            if (it == FitnessAction.Refresh) {
                                refreshes++
                                state = state.copy(loadState = FitnessLoadState.Loading())
                            }
                        },
                        onWorkout = {},
                    )
                }
            }
        }
        assertLoadingCentered()
        compose.runOnIdle {
            state = state.copy(loadState = FitnessLoadState.Ready, overview = FitnessOverview(day, emptyList(), emptyList()))
        }
        val calendar = compose.onNodeWithText("SELECTED DAY")
        val beforeRefresh = calendar.getUnclippedBoundsInRoot()
        compose.onNodeWithTag("fitness-viewport").performTouchInput { swipeDown() }
        compose.runOnIdle { assertEquals(1, refreshes) }
        compose.onNodeWithText("Refreshing your activity").assertDoesNotExist()
        assertEquals(beforeRefresh, calendar.getUnclippedBoundsInRoot())
        compose.runOnIdle {
            state = state.copy(loadState = FitnessLoadState.Failed(Failure.Connection))
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
