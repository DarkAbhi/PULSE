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
import dev.abhishekan.pulse.core.domain.model.WorkoutDetail
import dev.abhishekan.pulse.core.domain.model.WorkoutInfo
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.feature.fitness.WorkoutScreen
import dev.abhishekan.pulse.feature.fitness.WorkoutUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class WorkoutLoadingLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun initialLoaderIsCenteredAndRefreshKeepsDetailsInPlace() {
        var state by mutableStateOf(WorkoutUiState())
        var refreshes = 0
        compose.setContent {
            PulseTheme(false) {
                Box(Modifier.fillMaxSize().testTag("workout-viewport")) {
                    WorkoutScreen(
                        state = state,
                        onRefresh = {
                            refreshes++
                            state = state.copy(loading = true)
                        },
                        onEdit = {},
                        onDelete = {},
                        onDeleteExercise = {},
                        onReviewed = {},
                        onName = {},
                        onSet = { _, _, _ -> },
                        onAddSet = {},
                        onRemoveSet = {},
                        onChoose = {},
                        onClearMatch = {},
                        onSearch = {},
                        onAlias = {},
                        onSave = {},
                        onCancel = {},
                    )
                }
            }
        }
        val loader = compose.onNodeWithText("Loading workout")
        loader.assertIsDisplayed()
        val bounds = loader.getUnclippedBoundsInRoot()
        val viewport = compose.onNodeWithTag("workout-viewport").getUnclippedBoundsInRoot()
        assertEquals((viewport.left + viewport.right).value / 2, (bounds.left + bounds.right).value / 2, 1f)
        assertEquals((viewport.top + viewport.bottom).value / 2, (bounds.top + bounds.bottom).value / 2, 1f)
        compose.runOnIdle {
            state =
                state.copy(
                    loading = false,
                    detail =
                        WorkoutDetail(
                            WorkoutInfo(1, Instant.parse("2026-10-20T02:00:00Z"), null, null, null, null, false),
                            emptyList(),
                        ),
                )
        }
        val heading = compose.onNodeWithText("Timing and energy data aren’t available for this visit.")
        heading.assertIsDisplayed()
        val beforeRefresh = heading.getUnclippedBoundsInRoot()
        compose.onNodeWithText("Refresh workout").assertDoesNotExist()
        compose.onNodeWithTag("workout-refresh").performTouchInput { swipeDown() }
        compose.runOnIdle { assertEquals(1, refreshes) }
        loader.assertDoesNotExist()
        heading.assertIsDisplayed()
        assertEquals(beforeRefresh, heading.getUnclippedBoundsInRoot())
        compose.onNodeWithTag("workout-refresh").performTouchInput { swipeDown() }
        compose.runOnIdle {
            assertEquals(1, refreshes)
            state = state.copy(loading = false, busy = true)
        }
        compose.onNodeWithTag("workout-refresh").performTouchInput { swipeDown() }
        compose.runOnIdle {
            assertEquals(1, refreshes)
            state = state.copy(loading = false, busy = false, error = Failure.Connection)
        }
        loader.assertDoesNotExist()
        heading.assertExists()
    }
}
