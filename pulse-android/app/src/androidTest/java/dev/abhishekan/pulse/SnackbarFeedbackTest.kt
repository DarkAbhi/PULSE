package dev.abhishekan.pulse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseSnackbar
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarProvider
import dev.abhishekan.pulse.core.ui.components.PulseText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SnackbarFeedbackTest {
    @get:Rule val compose = createComposeRule()

    @Test fun feedbackDoesNotMoveContentAndCanRepeatWithoutReplayingOnReturn() {
        val host = SnackbarHostState()
        var message by mutableStateOf<UiText?>(null)
        var visible by mutableStateOf(true)
        var consumed = 0
        compose.setContent {
            PulseTheme(false) {
                Scaffold(snackbarHost = { SnackbarHost(host) { PulseSnackbar(it) } }) { padding ->
                    PulseSnackbarProvider(host) {
                        Box(Modifier.padding(padding)) {
                            PulseText("Workout content", Modifier.testTag("content"))
                            if (visible) {
                                PulseSnackbarEffect(message) {
                                    consumed++
                                    message = null
                                }
                            }
                        }
                    }
                }
            }
        }
        val content = compose.onNodeWithTag("content")
        val before = content.getUnclippedBoundsInRoot()
        compose.runOnIdle { message = UiText.Literal("Workout recorded") }
        compose.onNodeWithText("Workout recorded").assertIsDisplayed()
        assertEquals(before, content.getUnclippedBoundsInRoot())
        compose.runOnIdle {
            assertNull(message)
            assertEquals(1, consumed)
            host.currentSnackbarData?.dismiss()
        }
        compose.onNodeWithText("Workout recorded").assertDoesNotExist()
        compose.runOnIdle { visible = false }
        compose.runOnIdle { visible = true }
        compose.onNodeWithText("Workout recorded").assertDoesNotExist()
        compose.runOnIdle { message = UiText.Literal("Workout recorded") }
        compose.onNodeWithText("Workout recorded").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(2, consumed)
            host.currentSnackbarData?.dismiss()
        }
    }

    @Test fun feedbackDismissesAutomatically() {
        val host = SnackbarHostState()
        var message by mutableStateOf<UiText?>(UiText.Literal("Exercise saved"))
        compose.setContent {
            PulseTheme(false) {
                PulseSnackbarProvider(host) {
                    PulseSnackbarEffect(message) { message = null }
                    SnackbarHost(host) { PulseSnackbar(it) }
                }
            }
        }
        compose.onNodeWithText("Exercise saved").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(10_000)
        compose.onNodeWithText("Exercise saved").assertDoesNotExist()
    }
}
