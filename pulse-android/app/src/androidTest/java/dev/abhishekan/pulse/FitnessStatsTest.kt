package dev.abhishekan.pulse

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.domain.model.FitnessMetric
import dev.abhishekan.pulse.core.domain.model.FitnessOverview
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.feature.fitness.FitnessScreen
import dev.abhishekan.pulse.feature.fitness.FitnessUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

class FitnessStatsTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026, 10, 20)
    private val metrics =
        listOf(
            FitnessMetric("last_workout", "Last worked out", "20 days ago", "at gym", null),
            FitnessMetric("attendance", "Gym attendance", "6.48 %", "of days so far this year", null),
            FitnessMetric("workouts", "Total workouts", "20", "this year", null),
            FitnessMetric("streak", "Workout streak", "0 weeks", "with a gym visit", null),
            FitnessMetric("trained", "Total trained", "8h 43m", "This year", "2 days missing timing"),
            FitnessMetric("volume", "Volume lifted", "1,615 kg", "recorded sets this year · 2 sets missing weight", null),
            FitnessMetric("sets", "Sets", "16", "recorded this year", null),
            FitnessMetric("preference", "Preferred workout time", "Afternoon", "from recorded starts this year", null),
        )

    private fun show(
        dark: Boolean = false,
        fontScale: Float = 1f,
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                PulseTheme(dark) {
                    Box(Modifier.fillMaxSize().background(PulseTheme.colors.background)) {
                        FitnessScreen(
                            FitnessUiState(today, YearMonth.from(today), FitnessOverview(today, metrics, emptyList()), loading = false),
                            {},
                            {},
                            {},
                            {},
                            {},
                            {},
                            {},
                        )
                    }
                }
            }
        }
    }

    @Test fun allEightStatsFollowCalendarAndWorkouts() {
        show()
        compose.onNodeWithText("Workouts for this day").performScrollTo()
        val workouts = compose.onNodeWithText("Workouts for this day").getUnclippedBoundsInRoot()
        val stats = compose.onNodeWithText("Your fitness stats").getUnclippedBoundsInRoot()
        assertTrue(stats.top > workouts.bottom)
        compose.onNodeWithText("Your fitness stats").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("20 days ago", useUnmergedTree = true).performScrollTo()
        screenshot("light-overview")
        metrics.forEach { metric ->
            compose
                .onAllNodesWithText(metric.value, useUnmergedTree = true)
                .onLast()
                .performScrollTo()
                .assertIsDisplayed()
        }
        compose.onNodeWithText("Data coverage: 2 days missing timing", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(metrics.last().subtitle, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        screenshot("light-details")
    }

    @Test fun darkThemePreservesMissingDataAndLongValues() {
        show(dark = true)
        compose.onNodeWithText("Your fitness stats").performScrollTo()
        compose.onNodeWithText("20 days ago", useUnmergedTree = true).performScrollTo()
        screenshot("dark-overview")
        compose.onNodeWithText(metrics[5].subtitle, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Afternoon", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(metrics.last().subtitle, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        screenshot("dark-details")
    }

    @Test fun largeTextUsesOneColumnWithoutTruncatingStats() {
        show(fontScale = 2f)
        compose.onNodeWithText("Last worked out", useUnmergedTree = true).performScrollTo()
        val first = compose.onNodeWithText("Last worked out", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val second = compose.onNodeWithText("Gym attendance", useUnmergedTree = true).getUnclippedBoundsInRoot()
        // At phone widths accessibility text stacks the cards; wider tablets can still fit columns.
        if (InstrumentationRegistry
                .getInstrumentation()
                .targetContext.resources.configuration.screenWidthDp < 600
        ) {
            assertEquals(first.left.value, second.left.value, 1f)
            assertTrue(second.top > first.bottom)
        }
        screenshot("large-text")
        metrics.forEach { metric ->
            compose
                .onAllNodesWithText(metric.value, useUnmergedTree = true)
                .onLast()
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test fun refreshingKeepsStatsAndNewResponseReplacesThem() {
        var state by mutableStateOf(
            FitnessUiState(today, YearMonth.from(today), FitnessOverview(today, metrics, emptyList()), loading = false),
        )
        compose.setContent {
            PulseTheme(false) { FitnessScreen(state, {}, {}, {}, {}, {}, {}, {}) }
        }
        compose.runOnIdle { state = state.copy(loading = true) }
        compose.onNodeWithText("1,615 kg", useUnmergedTree = true).assertExists()
        compose.runOnIdle {
            state = state.copy(loading = false, overview = FitnessOverview(today, emptyList(), emptyList()))
        }
        compose.onNodeWithText("1,615 kg", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Stats are unavailable right now. Pull to refresh to try again.").performScrollTo().assertIsDisplayed()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "fitness-stats")
        folder.mkdirs()
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
