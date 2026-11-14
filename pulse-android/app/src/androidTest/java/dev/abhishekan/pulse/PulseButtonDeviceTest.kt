package dev.abhishekan.pulse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PulseButtonDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun stylesKeepLabelsTargetsAndInteractionStatesAcrossThemes() {
        var dark by mutableStateOf(false)
        var clicks = 0
        compose.setContent {
            PulseTheme(dark) {
                Column {
                    PulseButtonStyle.entries.forEach { style ->
                        listOf("enabled", "disabled", "loading").forEach { state ->
                            PulseButton(
                                "${style.name} $state",
                                { clicks++ },
                                enabled = state != "disabled",
                                loading = state == "loading",
                                style = style,
                                icon = { Box(Modifier.size(24.dp)) },
                            )
                        }
                    }
                }
            }
        }
        listOf(false, true).forEach { darkTheme ->
            compose.runOnIdle { dark = darkTheme }
            PulseButtonStyle.entries.forEach { style ->
                fun node(state: String) =
                    if (style == PulseButtonStyle.Icon) {
                        compose.onNodeWithContentDescription("${style.name} $state")
                    } else {
                        compose.onNodeWithText("${style.name} $state")
                    }
                node("enabled").assertIsEnabled().assertHeightIsAtLeast(48.dp).performClick()
                if (style == PulseButtonStyle.Icon) {
                    node("enabled").assertWidthIsAtLeast(48.dp)
                    compose.onNodeWithText("Icon enabled").assertDoesNotExist()
                }
                node("disabled").assertIsNotEnabled().performClick()
                node("loading").assertIsNotEnabled().performClick()
            }
        }
        compose.runOnIdle { assertEquals(6, clicks) }
    }
}
