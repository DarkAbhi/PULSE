package dev.abhishekan.pulse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.FitnessDates
import dev.abhishekan.pulse.core.domain.model.FitnessOverview
import dev.abhishekan.pulse.core.domain.model.WorkoutSummary
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.feature.fitness.overview.FitnessAction
import dev.abhishekan.pulse.feature.fitness.overview.FitnessScreen
import dev.abhishekan.pulse.feature.fitness.overview.FitnessUiState
import dev.abhishekan.pulse.feature.fitness.shared.FitnessLoadState
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

    @Test fun narrowPhonesKeepSevenColumns() = verifyFullWeek(320.dp)

    @Test fun allSevenColumnsFitAtS21Width() = verifyFullWeek(360.dp)

    @Test fun allSevenColumnsFitOnWidePhones() = verifyFullWeek(412.dp)

    @Test fun calendarRendersInLightTheme() = verifyFullWeek(360.dp, dark = false)

    @Test fun largeTextKeepsSevenColumns() = verifyFullWeek(320.dp, fontScale = 2f)

    @Test fun monthHeightAnimatesInBothDirections() {
        val shortMonth = YearMonth.of(2026, 10)
        val tallMonth = shortMonth.plusMonths(1)
        val month = mutableStateOf(shortMonth)
        val overview = FitnessOverview(tallMonth.atEndOfMonth(), emptyList(), emptyList())
        compose.setContent {
            PulseTheme(true) {
                Box(Modifier.width(360.dp)) {
                    FitnessScreen(
                        FitnessUiState(month.value.atDay(1), month.value, overview, loadState = FitnessLoadState.Ready),
                        {},
                        {},
                    )
                }
            }
        }
        val legend = compose.onNodeWithText("Workout logged")

        fun calendarBottom() = legend.getUnclippedBoundsInRoot().top

        val shortHeight = calendarBottom()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { month.value = tallMonth }
        compose.mainClock.advanceTimeBy(96)
        val expanding = calendarBottom()
        compose.mainClock.advanceTimeBy(240)
        val tallHeight = calendarBottom()
        assertTrue("Calendar should pass through an intermediate height when growing", expanding > shortHeight && expanding < tallHeight)

        compose.runOnIdle { month.value = shortMonth }
        compose.mainClock.advanceTimeBy(96)
        val collapsing = calendarBottom()
        compose.mainClock.advanceTimeBy(240)
        assertTrue(
            "Calendar should pass through an intermediate height when shrinking",
            collapsing > shortHeight && collapsing < tallHeight,
        )
        assertEquals(shortHeight, calendarBottom())
    }

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
                                loadState = FitnessLoadState.Ready,
                            ),
                            { if (it is FitnessAction.SelectDate) selected = it.date },
                            {},
                        )
                    }
                }
            }
        }
        val viewport = compose.onNodeWithTag("viewport").getUnclippedBoundsInRoot()
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
        val weeks = FitnessDates.calendar(month).chunked(7)

        fun dateNode(date: LocalDate) =
            compose.onNode(hasContentDescription(date.format(formatter) + ",", substring = true) and hasClickAction())

        weeks.forEach { week ->
            val bounds =
                week.map { date ->
                    val node = dateNode(date)
                    node.performScrollTo().assertIsDisplayed()
                    val layout = node.getUnclippedBoundsInRoot()
                    assertTrue("$date extends past the viewport", layout.left >= viewport.left && layout.right <= viewport.right)
                    assertTrue("$date has a usable row height", layout.bottom - layout.top >= 44.dp)
                    layout
                }
            bounds.zipWithNext().forEach { (first, second) ->
                assertEquals(first.top.value, second.top.value, 0.5f)
                assertTrue("Day columns must not overlap", first.right <= second.left)
            }
            dateNode(week.first()).performScrollTo().performTouchInput { click(center) }
            compose.runOnIdle { assertEquals(week.first(), selected) }
        }
        compose.onNodeWithContentDescription("Previous month").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Next month").assertIsDisplayed()
        compose.onNodeWithContentDescription("Choose date").assertDoesNotExist()
    }
}
