package dev.abhishekan.pulse

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.ui.PulseDarkColors
import dev.abhishekan.pulse.core.ui.PulseLightColors
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.gallery.PulseFoundationBoard
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class DesignSystemDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun themeSwitchesWithoutLosingControlState() {
        var dark by mutableStateOf(false)
        var clicks = 0
        compose.setContent {
            PulseTheme(dark) {
                assertEquals(if (dark) PulseDarkColors else PulseLightColors, PulseTheme.colors)
                assertEquals(PulseTheme.colors.textPrimary, LocalContentColor.current)
                Column {
                    PulseButton("Action", { clicks++ })
                    PulseButton("Disabled action", { clicks++ }, enabled = false)
                    PulseButton("Saving action", { clicks++ }, loading = true)
                    PulseButton("Delete action", { clicks++ }, variant = PulseButtonVariant.Destructive)
                }
            }
        }
        compose.onNodeWithText("Action").performClick()
        compose.onNodeWithText("Disabled action").assertIsNotEnabled()
        compose.onNodeWithText("Saving action").assertIsNotEnabled()
        compose.runOnIdle { dark = true }
        compose.onNodeWithText("Action").performClick()
        compose.onNodeWithText("Delete action").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(3, clicks) }
    }

    @Test fun lightFoundationRenders() = specimen(false, 1f, "foundation-light")

    @Test fun darkFoundationRenders() = specimen(true, 1f, "foundation-dark")

    @Test fun doubledTextRemainsReachable() = specimen(false, 2f, "foundation-large-text")

    private fun specimen(
        dark: Boolean,
        fontScale: Float,
        name: String,
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                PulseTheme(dark) {
                    Surface(color = PulseTheme.colors.background, contentColor = PulseTheme.colors.textPrimary) {
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PulseTheme.spacing.medium),
                            verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.medium),
                        ) {
                            PulseFoundationBoard()
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Room for everyday life.").assertIsDisplayed()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose
            .onNodeWithText("Continue")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
        compose.onNodeWithText("A small intention").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Today").performScrollTo().assertIsSelected()
        compose.onNodeWithText("● Ready when you are").performScrollTo().assertIsDisplayed()
    }
}
