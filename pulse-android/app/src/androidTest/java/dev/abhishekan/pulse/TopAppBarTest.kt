package dev.abhishekan.pulse

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseScreenScaffold
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TopAppBarTest {
    @get:Rule val compose = createComposeRule()

    @Test fun appBarBackUsesTheSameNestedHandlerAsSystemBack() {
        var nested by mutableStateOf(false)
        var step by mutableStateOf(2)
        var busy by mutableStateOf(false)
        var systemBack: () -> Unit = {}
        compose.setContent {
            val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            systemBack = { dispatcher.onBackPressed() }
            PulseTheme {
                PulseScreenScaffold("Add vehicle", showTopBar = nested, backLabel = "Back", backEnabled = !busy) {
                    BackHandler(nested) {
                        if (!busy) {
                            if (step > 0) step-- else nested = false
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("pulse-top-app-bar").assertDoesNotExist()
        compose.runOnIdle { nested = true }
        compose.onNodeWithText("Add vehicle").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle {
            assertEquals(1, step)
            busy = true
        }
        compose.onNodeWithContentDescription("Back").assertIsNotEnabled()
        compose.runOnIdle {
            systemBack()
            assertEquals(1, step)
            busy = false
        }
        compose.runOnIdle { systemBack() }
        compose.runOnIdle { assertEquals(0, step) }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("pulse-top-app-bar").assertDoesNotExist()
    }
}
