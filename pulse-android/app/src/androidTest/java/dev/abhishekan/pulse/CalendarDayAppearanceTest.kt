package dev.abhishekan.pulse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseDarkColors
import dev.abhishekan.pulse.core.ui.PulseLightColors
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseCalendarDay
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CalendarDayAppearanceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun circularMarkersInLightTheme() = verifyMarkers(false)

    @Test fun circularMarkersInDarkTheme() = verifyMarkers(true)

    private fun verifyMarkers(dark: Boolean) {
        val colors = if (dark) PulseDarkColors else PulseLightColors
        var clicks = 0
        compose.setContent {
            PulseTheme(dark) {
                Row(Modifier.background(colors.background)) {
                    // Wide columns expose regressions where the highlight stretches.
                    repeat(4) { state ->
                        PulseCalendarDay(
                            label = "28",
                            description = "Calendar state $state",
                            selected = state >= 2,
                            today = state == 3,
                            hasActivity = state == 1 || state == 3,
                            onClick = { clicks++ },
                            modifier = Modifier.width(80.dp).testTag("day-$state"),
                            inMonth = state != 1,
                        )
                    }
                }
            }
        }
        repeat(4) { state ->
            val node = compose.onNodeWithTag("day-$state")
            val pixels = node.captureToImage().toPixelMap()

            fun colorAt(
                x: Int,
                y: Int,
            ): Color = pixels[(pixels.width * x / 80), (pixels.height * y / 56)]

            // Both sides of the centered circle remain clear, even for selection.
            assertEquals(colors.background, colorAt(12, 28))
            assertEquals(colors.background, colorAt(68, 28))
            assertEquals(colors.background, colorAt(22, 10))
            assertEquals(
                when (state) {
                    0 -> colors.background
                    1 -> colors.successSoft
                    else -> colors.primary
                },
                colorAt(40, 12),
            )
            if (state == 1 || state == 3) assertEquals(colors.success, colorAt(40, 48))
            if (state == 3) assertEquals(colors.action, colorAt(40, 7))
            if (state >= 2) node.assertIsSelected()
            node.performClick()
        }
        compose.runOnIdle { assertEquals(4, clicks) }
    }
}
