package dev.abhishekan.pulse

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseBottomSheet
import dev.abhishekan.pulse.core.ui.components.PulseText
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class PulseBottomSheetTest {
    @get:Rule val compose = createComposeRule()
    private var windowHeight = 0

    @Test fun halfOpenExpandsAndSwipesAwayInLightTheme() = verifyGestures(false)

    @Test fun halfOpenExpandsAndSwipesAwayInDarkTheme() = verifyGestures(true)

    @Test fun fullScreenShowsTitleAndCloseDismisses() {
        showSheet(fullScreen = true)
        capture("full")
        assertTrue(titleY() < windowHeight * 0.25f)
        compose.onNodeWithContentDescription("Close bottom sheet").assertIsDisplayed().performClick()
        compose.onNodeWithText("Sheet title").assertDoesNotExist()
    }

    private fun verifyGestures(dark: Boolean) {
        showSheet(dark = dark)
        capture("half-$dark")
        assertTrue("Default sheet opens halfway: ${titleY()} / $windowHeight", titleY() in windowHeight * 0.45f..windowHeight * 0.75f)
        compose.onNodeWithText("Sheet title").performTouchInput {
            swipe(center, Offset(center.x, center.y - windowHeight * 0.6f), 500)
        }
        compose.waitForIdle()
        capture("expanded-$dark")
        assertTrue("Upward drag expands sheet", titleY() < windowHeight * 0.25f)
        // One downward drag may stop at the half-height anchor; the next dismisses.
        repeat(2) {
            if (compose.onAllNodes(hasText("Sheet title")).fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithText("Sheet title").performTouchInput {
                    swipe(center, Offset(center.x, center.y + windowHeight * 0.8f), 300)
                }
                compose.waitForIdle()
            }
        }
        compose.onNodeWithText("Sheet title").assertDoesNotExist()
    }

    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        File(
            InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
                ?: requireNotNull(instrumentation.targetContext.externalCacheDir).path,
            "pulse-sheet-$name.png",
        ).outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun titleY() =
        compose
            .onNodeWithText("Sheet title")
            .fetchSemanticsNode()
            .positionInWindow.y

    private fun showSheet(
        fullScreen: Boolean = false,
        dark: Boolean = false,
    ) {
        compose.setContent {
            windowHeight = LocalWindowInfo.current.containerSize.height
            var open by remember { mutableStateOf(true) }
            PulseTheme(dark) {
                Box(Modifier.fillMaxSize()) {
                    if (open) {
                        PulseBottomSheet("Sheet title", { open = false }, fullScreen = fullScreen) {
                            PulseText("Sheet content")
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Sheet title").assertIsDisplayed()
    }
}
