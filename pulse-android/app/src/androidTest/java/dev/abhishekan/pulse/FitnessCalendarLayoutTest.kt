package dev.abhishekan.pulse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.domain.model.FitnessDates
import dev.abhishekan.pulse.core.domain.model.FitnessOverview
import dev.abhishekan.pulse.core.domain.model.WorkoutSummary
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.feature.fitness.FitnessScreen
import dev.abhishekan.pulse.feature.fitness.FitnessUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class FitnessCalendarLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun narrowPhonesUseFullWidthDateRows() = verifyFullWeek(320.dp)

    @Test fun sevenColumnsFitAtTheMinimumGridWidth() = verifyFullWeek(344.dp)

    @Test fun allSevenColumnsFitAtS21Width() = verifyFullWeek(360.dp)

    @Test fun allSevenColumnsFitOnWidePhones() = verifyFullWeek(412.dp)

    @Test fun calendarRendersInLightTheme() = verifyFullWeek(360.dp, dark = false)

    @Test fun largeTextKeepsDateRowsAndDateSelection() = verifyFullWeek(320.dp, fontScale = 2f)

    private fun verifyFullWeek(
        width: Dp,
        fontScale: Float = 1f,
        dark: Boolean = true,
    ) {
        val today = LocalDate.of(2026, 10, 28)
        val month = YearMonth.from(today)
        var selected: LocalDate? = null
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                PulseTheme(dark) {
                    Box(
                        Modifier
                            .width(width)
                            .fillMaxHeight()
                            .background(PulseTheme.colors.background)
                            .testTag("viewport"),
                    ) {
                        FitnessScreen(
                            FitnessUiState(
                                today,
                                month,
                                FitnessOverview(
                                    today,
                                    emptyList(),
                                    listOf(
                                        WorkoutSummary(1, today, "Morning"),
                                        WorkoutSummary(2, LocalDate.of(2026, 9, 28), "Morning"),
                                        WorkoutSummary(3, LocalDate.of(2026, 9, 30), "Morning"),
                                    ),
                                ),
                                loading = false,
                            ),
                            { selected = it },
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
        val viewport = compose.onNodeWithTag("viewport").getUnclippedBoundsInRoot()
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
        val grid = width >= 344.dp
        val weeks = FitnessDates.calendar(month).chunked(7)
        val deviceDensity =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext.resources.displayMetrics.density

        fun dateNode(date: LocalDate) =
            compose.onNode(hasContentDescription(date.format(formatter) + ",", substring = true) and hasClickAction())

        if (!grid) {
            repeat(weeks.indexOfFirst { today in it }) {
                compose.onNodeWithContentDescription("Previous week").performScrollTo().performClick()
            }
        }
        weeks.forEachIndexed { index, week ->
            val bounds =
                week.map { date ->
                    val node = dateNode(date)
                    node.performScrollTo().assertIsDisplayed()
                    val layout = node.getUnclippedBoundsInRoot()
                    val touch = node.fetchSemanticsNode().touchBoundsInRoot
                    assertTrue("$date extends past the viewport", layout.left >= viewport.left && layout.right <= viewport.right)
                    assertTrue(
                        "$date needs 48 dp layout bounds",
                        layout.right - layout.left >= 48.dp && layout.bottom - layout.top >= 48.dp,
                    )
                    assertTrue("$date needs 48 dp touch bounds", touch.width >= 48 * deviceDensity && touch.height >= 48 * deviceDensity)
                    layout
                }
            bounds.zipWithNext().forEach { (first, second) ->
                if (grid) {
                    assertEquals(first.top.value, second.top.value, 0.5f)
                    assertTrue("Day columns must not overlap", first.right <= second.left)
                } else {
                    assertEquals(first.left.value, second.left.value, 0.5f)
                    assertTrue("Date rows must not overlap", first.bottom <= second.top)
                }
            }
            val first = week.first()
            dateNode(first).performScrollTo().performTouchInput {
                click(if (grid) Offset(right - 2f, center.y) else Offset(center.x, bottom - 2f))
            }
            compose.runOnIdle { assertEquals(first, selected) }
            if (!grid && index < weeks.lastIndex) {
                compose.onNodeWithContentDescription("Next week").performScrollTo().performClick()
            }
        }
        compose.onNodeWithContentDescription("Choose date").performScrollTo()
        if (!grid) {
            compose.onNodeWithContentDescription("Previous week").performScrollTo().performClick()
            compose.onNode(hasText("Today") and hasClickAction()).performScrollTo().performClick()
            dateNode(today).performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Choose date").performScrollTo()
        }
        compose.onNodeWithText("Swipe the calendar sideways", substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Choose date").assertIsDisplayed()
        if (fontScale == 2f) {
            compose.onNodeWithContentDescription("Choose date").performClick()
            compose.onNode(isDialog()).assertExists()
            compose.onNode(hasSetTextAction()).assertIsDisplayed()
            compose.onNodeWithText("Cancel").performClick()
        }
        val shortcut = compose.onNode(hasText("View selected day") and hasClickAction())
        shortcut.performScrollTo().performClick()
        compose.onNodeWithContentDescription("${today.format(formatter)}, 1 workout").assertIsDisplayed()
    }
}
