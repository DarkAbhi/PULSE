package dev.abhishekan.pulse

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.domain.model.FitnessDates
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
import java.time.format.DateTimeFormatter
import java.util.Locale

class FitnessCalendarLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allSevenColumnsFitOnSmallPhones() = verifyFullWeek(320.dp)

    @Test fun allSevenColumnsFitAtS21Width() = verifyFullWeek(360.dp)

    @Test fun allSevenColumnsFitOnWidePhones() = verifyFullWeek(412.dp)

    @Test fun largeTextKeepsTheFullWeekAndDateSelection() = verifyFullWeek(320.dp, fontScale = 2f)

    private fun verifyFullWeek(
        width: Dp,
        fontScale: Float = 1f,
    ) {
        val today = LocalDate.of(2026, 10, 28)
        val month = YearMonth.from(today)
        var selected: LocalDate? = null
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                PulseTheme(true) {
                    Box(
                        Modifier
                            .width(width)
                            .fillMaxHeight()
                            .background(PulseTheme.colors.background)
                            .testTag("viewport"),
                    ) {
                        FitnessScreen(
                            FitnessUiState(today, month, FitnessOverview(today, emptyList(), emptyList()), loading = false),
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
        FitnessDates.calendar(month).chunked(7).forEach { week ->
            val bounds =
                week.map { date ->
                    val node = compose.onNodeWithContentDescription(date.format(formatter), substring = true)
                    node.assertIsDisplayed()
                    node.getUnclippedBoundsInRoot().also {
                        assertTrue("$date extends past the left edge", it.left >= viewport.left)
                        assertTrue("$date extends past the right edge", it.right <= viewport.right)
                        assertTrue("$date must have a usable tap target", it.right - it.left >= 44.dp && it.bottom - it.top >= 48.dp)
                    }
                }
            bounds.zipWithNext().forEach { (left, right) ->
                assertEquals(left.top.value, right.top.value, 0.5f)
                assertTrue("Day columns must not overlap: $left, $right", left.right <= right.left + 0.5.dp)
            }
        }
        val sunday = LocalDate.of(2026, 10, 4)
        compose.onNodeWithContentDescription(sunday.format(formatter), substring = true).performClick()
        compose.runOnIdle { assertEquals(sunday, selected) }
        compose.onNodeWithText("Swipe the calendar sideways", substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Choose date").assertIsDisplayed()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "calendar-${width.value.toInt()}-$fontScale.png").outputStream().use {
            compose
                .onNodeWithTag("viewport")
                .captureToImage()
                .asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
