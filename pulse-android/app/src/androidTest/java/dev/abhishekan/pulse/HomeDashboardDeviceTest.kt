package dev.abhishekan.pulse

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Horizon
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.feature.home.HomeScreen
import dev.abhishekan.pulse.feature.home.HomeUiState
import dev.abhishekan.pulse.feature.home.Section
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.math.BigDecimal

class HomeDashboardDeviceTest {
    @get:Rule val compose = createComposeRule()

    private val state =
        HomeUiState(
            profile = Section.Content(Profile("Abhishek")),
            workout = Section.Empty,
            horizon =
                Section.Content(
                    Horizon("₹", BigDecimal("150000"), BigDecimal("100000.125"), BigDecimal("50000")),
                ),
        )

    private fun show(
        dark: Boolean = false,
        fontScale: Float = 1f,
        failure: Boolean = false,
        onRetry: () -> Unit = {},
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                PulseTheme(dark) {
                    HomeScreen(
                        "fixture",
                        if (failure) state.copy(horizon = Section.Error(Failure.Connection)) else state,
                        {},
                        {},
                        onRetry,
                        {},
                        {},
                        {},
                    )
                }
            }
        }
    }

    private fun assertMatchingCardHeights() {
        val finance = compose.onNodeWithContentDescription("Financial Horizon").getUnclippedBoundsInRoot()
        val fitness = compose.onNodeWithContentDescription("FITNESS · TODAY").getUnclippedBoundsInRoot()
        assertEquals((finance.bottom - finance.top).value, (fitness.bottom - fitness.top).value, 0.5f)
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.waitForIdle(500, 5_000)
        val bitmap = checkNotNull(automation.takeScreenshot())
        File(context.getExternalFilesDir(null), "dashboard-$name.png").outputStream().use {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        bitmap.recycle()
    }

    @Test fun workoutAndBalanceRemainVisibleDuringRefreshAndAfterRefreshFailure() {
        val current = mutableStateOf(state)
        compose.setContent {
            PulseTheme(false) {
                HomeScreen("fixture", current.value, {}, {}, {}, {}, {}, {})
            }
        }
        compose.runOnIdle { current.value = state.copy(refreshing = setOf("workout", "horizon")) }
        compose.onNodeWithText("No workout recorded today.").assertExists()
        compose.onNodeWithText("Loading today’s workout").assertDoesNotExist()
        compose.onNodeWithText("Loading your horizon").assertDoesNotExist()
        assertMatchingCardHeights()
        compose.runOnIdle {
            current.value = state.copy(refreshErrors = mapOf("workout" to Failure.Connection))
        }
        compose.onNodeWithText("No workout recorded today.").assertExists()
    }

    @Test fun pageGuttersStayConsistentAcrossCompactAndWideLayouts() {
        val width = mutableStateOf(360.dp)
        compose.setContent {
            PulseTheme(false) {
                Box(Modifier.requiredWidth(width.value).fillMaxHeight()) {
                    HomeScreen("fixture", state, {}, {}, {}, {}, {}, {})
                }
            }
        }
        listOf(360.dp, 599.dp, 600.dp, 840.dp, 1440.dp).forEach { pageWidth ->
            compose.runOnIdle { width.value = pageWidth }
            compose.waitForIdle()
            val viewport = compose.onNodeWithTag("dashboard-background").getUnclippedBoundsInRoot()
            val greeting = compose.onNodeWithTag("dashboard-greeting").getUnclippedBoundsInRoot()
            val padding = if (pageWidth < 600.dp) 16.dp else 32.dp
            val gutter = (pageWidth - minOf(pageWidth, 1280.dp)) / 2 + padding
            assertEquals(gutter.value, (greeting.left - viewport.left).value, 1f)
            assertEquals(gutter.value, (viewport.right - greeting.right).value, 1f)
            assertEquals(padding.value, (greeting.top - viewport.top).value, 1f)
        }
    }

    @Test fun lightDashboardKeepsExactBalanceAndActions() {
        var horizon = 0
        var fitness = 0
        var notifications = 0
        var profile = 0
        compose.setContent {
            PulseTheme(false) {
                HomeScreen("fixture", state, {}, {}, {}, { horizon++ }, { fitness++ }, { notifications++ }, { profile++ })
            }
        }
        compose.onNodeWithTag("dashboard-greeting").assertIsDisplayed()
        compose.onNodeWithText("Pulse", substring = false).assertDoesNotExist()
        compose.onNodeWithText("Good to see you,").assertIsDisplayed()
        compose.onNodeWithText("Abhishek").assertIsDisplayed()
        assertMatchingCardHeights()
        capture("light")
        compose.onNodeWithContentDescription("Notifications").assertHasClickAction().performClick()
        compose.onNodeWithContentDescription("Profile").assertHasClickAction().performClick()
        compose.onNodeWithText("₹ 100000.125").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Financial Horizon").performScrollTo().performClick()
        compose.onNodeWithContentDescription("FITNESS · TODAY").performScrollTo().performClick()
        assertEquals(1, horizon)
        assertEquals(1, fitness)
        assertEquals(1, notifications)
        assertEquals(1, profile)
    }

    @Test fun orbBreathesAndRipplesBehindReadableGreeting() {
        compose.mainClock.autoAdvance = false
        show()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Abhishek").assertIsDisplayed()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.waitForIdle(500, 5_000)
        val before = checkNotNull(automation.takeScreenshot())
        compose.mainClock.advanceTimeBy(1_500)
        compose.waitForIdle()
        compose.waitUntil(5_000) {
            val after = checkNotNull(automation.takeScreenshot())
            var changed = 0
            for (y in 0 until before.height / 2 step 8) {
                for (x in before.width / 2 until before.width step 8) {
                    if (before.getPixel(x, y) != after.getPixel(x, y)) changed++
                }
            }
            after.recycle()
            changed > 50
        }
        before.recycle()
        capture("orb-quarter-cycle")
        compose.onNodeWithText("Good to see you,").assertIsDisplayed()
        compose.onNodeWithText("Abhishek").assertIsDisplayed()
    }

    @Test fun darkDashboardKeepsWorkoutReadable() {
        show(dark = true)
        assertMatchingCardHeights()
        capture("dark")
        compose.onNodeWithText("No workout recorded today.").performScrollTo().assertIsDisplayed()
    }

    @Test fun largeTextKeepsCardsScrollable() {
        show(fontScale = 2f)
        compose.onNodeWithText("Abhishek").performScrollTo().assertIsDisplayed()
        capture("large-text-greeting")
        compose.onNodeWithText("₹ 100000.125").performScrollTo().assertIsDisplayed()
        assertMatchingCardHeights()
        capture("large-text")
        compose.onNodeWithText("Let’s get moving").performScrollTo().assertIsDisplayed()
    }

    @Test fun failedFinanceStillRetriesWithoutOpeningCard() {
        var retries = 0
        show(failure = true, onRetry = { retries++ })
        assertMatchingCardHeights()
        compose.onNodeWithText("Try again").performScrollTo().performClick()
        assertEquals(1, retries)
        compose.onNodeWithText("No workout recorded today.").performScrollTo().assertIsDisplayed()
    }
}
