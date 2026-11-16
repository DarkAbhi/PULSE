package dev.abhishekan.pulse

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.domain.model.WorkoutDetail
import dev.abhishekan.pulse.core.domain.model.WorkoutExercise
import dev.abhishekan.pulse.core.domain.model.WorkoutInfo
import dev.abhishekan.pulse.core.domain.model.WorkoutSet
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.feature.fitness.shared.FitnessLoadState
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutScreen
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset

class WorkoutRedesignTest {
    @get:Rule val compose = createComposeRule()
    private val instant = Instant.parse("2026-09-28T12:31:00Z")
    private val catalog =
        CatalogExercise(
            "squat",
            "Barbell Squat",
            "barbell",
            listOf("quadriceps"),
            listOf(
                "Stand with your feet shoulder-width apart.",
                "Bend your knees and lower with control.",
                "Return to the starting position.",
            ),
        )
    private val exercise = WorkoutExercise(1, "Barbell Squat", "squat", catalog, (1..3).map { WorkoutSet(it, 10, BigDecimal.TEN) })
    private val manual = WorkoutInfo(1, instant, null, null, null, null, false)

    @Test fun linkedEmptyWorkoutShowsImportedMetricsAndAddAction() {
        val info = WorkoutInfo(1, instant, instant, instant.plusSeconds(1800), BigDecimal("1800"), BigDecimal("169.9"), true)
        compose.setContent { Content(WorkoutDetail(info, emptyList())) }
        compose.onNodeWithText("Linked Fitness Watch workout").assertIsDisplayed()
        compose.onNodeWithText("Imported timing and energy are read-only.").assertDoesNotExist()
        compose.onNodeWithText("Times shown in India time (IST).").assertDoesNotExist()
        compose.onNodeWithText("Refresh workout").assertDoesNotExist()
        compose.onNodeWithText("169.9 kcal").assertIsDisplayed()
        compose.onNodeWithText("30 min").assertIsDisplayed()
        compose.onNodeWithText("No exercises added yet").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Add exercise").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("YOUR WORKOUT").performScrollTo()
        screenshot("workout-linked-empty")
    }

    @Test fun overnightWorkoutKeepsTimesOnOneLineAtPhoneWidth() {
        val start = Instant.parse("2026-09-28T23:50:00Z")
        val info = WorkoutInfo(1, start, start, start.plusSeconds(1800), BigDecimal("1800"), BigDecimal("169.9"), true)
        compose.setContent {
            Box(Modifier.width(320.dp)) { Content(WorkoutDetail(info, emptyList())) }
        }

        for (text in listOf("Started", "23:50", "30 min", "Finished", "00:20", "169.9 kcal")) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(text).assertIsDisplayed().performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(text, 1, layouts.single().lineCount)
            assertFalse(text, layouts.single().isLineEllipsized(0))
        }
        val started = compose.onNodeWithText("23:50").getUnclippedBoundsInRoot()
        val duration = compose.onNodeWithText("30 min").getUnclippedBoundsInRoot()
        val finished = compose.onNodeWithText("00:20").getUnclippedBoundsInRoot()
        assertEquals(started.top, finished.top)
        assertTrue(started.right < duration.left && duration.right < finished.left)
        compose.onNodeWithText("29 Sep").assertIsDisplayed()
    }

    @Test fun populatedWorkoutHasCountsAndSheetInstructions() {
        compose.setContent { Content(WorkoutDetail(manual, listOf(exercise))) }
        compose.onNodeWithText("Timing and energy data aren’t available for this visit.").assertIsDisplayed()
        compose.onNodeWithText("WORKOUT SUMMARY").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("exercise", substring = false).assertExists()
        compose.onNodeWithText("sets", substring = false).assertExists()
        compose.onNodeWithTag("workout-summary-sets").assertTextEquals("3")
        compose.onNodeWithText("Great start!").assertDoesNotExist()
        compose.onNodeWithText(catalog.instructions.first()).assertDoesNotExist()
        compose.onNodeWithText("View instructions").performScrollTo()
        screenshot("workout-populated")
        compose.onNodeWithText("View instructions").performClick()
        compose.onNodeWithText("1. ${catalog.instructions.first()}").assertIsDisplayed()
        screenshot("workout-instructions")
        compose.onNodeWithContentDescription("Close sheet").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithText("1. ${catalog.instructions.first()}").assertDoesNotExist()
        compose.onNodeWithText("View instructions").assertIsDisplayed()
    }

    @Test fun customExerciseNeedsNoCatalogueAndRemoveStillConfirms() {
        compose.setContent {
            Content(WorkoutDetail(manual, listOf(exercise.copy(catalogId = null, catalog = null, name = "My exercise"))), dark = true)
        }
        compose.onNodeWithText("My exercise").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("View instructions").assertDoesNotExist()
        screenshot("workout-custom-dark")
        compose.onNodeWithText("Remove", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Delete this exercise?").assertIsDisplayed()
    }

    @Composable private fun Content(
        detail: WorkoutDetail,
        dark: Boolean = false,
    ) {
        PulseTheme(dark) {
            Box(Modifier.fillMaxSize().background(PulseTheme.colors.background)) {
                WorkoutScreen(
                    WorkoutUiState(detail = detail, loadState = FitnessLoadState.Ready),
                    onAction = {},
                    onExercise = {},
                    timeZone = ZoneOffset.UTC,
                )
            }
        }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "workout-redesign")
        folder.mkdirs()
        // Native dialog/window fades run outside Compose's test clock.
        SystemClock.sleep(400)
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
