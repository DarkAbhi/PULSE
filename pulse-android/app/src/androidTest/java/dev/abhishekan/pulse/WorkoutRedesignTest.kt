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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.domain.model.WorkoutClassification
import dev.abhishekan.pulse.core.domain.model.WorkoutDetail
import dev.abhishekan.pulse.core.domain.model.WorkoutExercise
import dev.abhishekan.pulse.core.domain.model.WorkoutInfo
import dev.abhishekan.pulse.core.domain.model.WorkoutSet
import dev.abhishekan.pulse.core.domain.model.WorkoutType
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.feature.fitness.shared.FitnessLoadState
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutAction
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
            category = "strength",
        )
    private val exercise = WorkoutExercise(1, "Barbell Squat", "squat", catalog, (1..3).map { WorkoutSet(it, 10, BigDecimal.TEN) })
    private val manual = WorkoutInfo(1, instant, null, null, null, null, false)

    @Test fun linkedEmptyWorkoutShowsImportedMetricsAndAddAction() {
        val info = WorkoutInfo(1, instant, instant, instant.plusSeconds(1800), BigDecimal("1800"), BigDecimal("169.9"), true)
        compose.setContent { Content(WorkoutDetail(info, emptyList())) }
        compose.onNodeWithText("Synced from watch").assertIsDisplayed()
        compose.onNodeWithText("Refresh workout").assertDoesNotExist()
        showWorkoutText("169.9 kcal")
        showWorkoutText("30 min")
        showWorkoutText("Nothing added to this workout yet")
        showWorkoutText("Add exercise")
        screenshot("workout-linked-empty")
    }

    @Test fun overnightWorkoutUsesStartDateAndTwoMetricColumnsAtPhoneWidth() {
        val start = Instant.parse("2026-09-28T23:50:00Z")
        val info = WorkoutInfo(1, start, start, start.plusSeconds(1800), BigDecimal("1800"), BigDecimal("169.9"), true)
        compose.setContent {
            Box(Modifier.width(320.dp)) { Content(WorkoutDetail(info, emptyList())) }
        }

        for (text in listOf("Start", "End", "30 min", "169.9 kcal")) {
            val layouts = mutableListOf<TextLayoutResult>()
            val node = compose.onNodeWithText(text)
            compose.scrollToDisplayed(node)
            node.assertIsDisplayed().performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(text, 1, layouts.single().lineCount)
            assertFalse(text, layouts.single().isLineEllipsized(0))
        }
        val started = compose.onNodeWithText("Start").getUnclippedBoundsInRoot()
        val finished = compose.onNodeWithText("End").getUnclippedBoundsInRoot()
        val duration = compose.onNodeWithText("30 min").getUnclippedBoundsInRoot()
        assertEquals(started.top, finished.top)
        assertTrue(started.right < finished.left && duration.top > started.bottom)
        compose.onNodeWithText("28").assertIsDisplayed()
    }

    @Test fun populatedWorkoutHasCountsAndSheetInstructions() {
        compose.setContent { Content(WorkoutDetail(manual, listOf(exercise))) }
        compose.onNodeWithText("WORKOUT SUMMARY").assertDoesNotExist()
        showWorkoutText("1 exercise · 3 sets · 300 kg lifted")
        showWorkoutText("Strength")
        compose.onNodeWithText(catalog.instructions.first()).assertDoesNotExist()
        showWorkoutText("Instructions")
        screenshot("workout-populated")
        compose.onNodeWithText("Instructions").performClick()
        compose.onNodeWithText(catalog.instructions.first()).assertIsDisplayed()
        val gotIt = compose.onNodeWithText("Got it")
        compose.scrollToDisplayed(gotIt)
        screenshot("workout-instructions")
        gotIt.performClick()
        compose.onNodeWithText(catalog.instructions.first()).assertDoesNotExist()
        compose.onNodeWithText("Instructions").assertIsDisplayed()
    }

    @Test fun instructionsButtonFitsNextToExerciseActionsOnPhone() {
        compose.setContent {
            Box(Modifier.width(320.dp)) { Content(WorkoutDetail(manual, listOf(exercise))) }
        }

        showWorkoutText("Instructions")
        val instructions = compose.onNodeWithText("Instructions")
        val layouts = mutableListOf<TextLayoutResult>()
        instructions.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(1, layouts.single().lineCount)
        assertFalse(layouts.single().isLineEllipsized(0))
        val edit = compose.onNodeWithContentDescription("Edit").getUnclippedBoundsInRoot()
        assertTrue(instructions.getUnclippedBoundsInRoot().right <= edit.left)
    }

    @Test fun customExerciseNeedsNoCatalogueAndRemoveStillConfirms() {
        compose.setContent {
            Content(WorkoutDetail(manual, listOf(exercise.copy(catalogId = null, catalog = null, name = "My exercise"))), dark = true)
        }
        showWorkoutText("My exercise")
        compose.onNodeWithText("Instructions").assertDoesNotExist()
        screenshot("workout-custom-dark")
        compose.onNodeWithTag("workout-list").performScrollToNode(hasContentDescription("Remove"))
        compose.onNodeWithContentDescription("Remove").performClick()
        compose.onNodeWithText("Delete this exercise?").assertIsDisplayed()
    }

    @Test fun exerciseCardsDisplaySetsDirectly() {
        val second =
            WorkoutExercise(
                2,
                "Front Plate Raise",
                null,
                null,
                listOf(WorkoutSet(1, 8, BigDecimal("7.5")), WorkoutSet(2, 12, BigDecimal.TEN)),
            )
        compose.setContent { Content(WorkoutDetail(manual, listOf(exercise, second))) }

        showWorkoutText("2 exercises · 5 sets · 480 kg lifted")
        compose.onNodeWithTag("workout-list").performScrollToNode(hasText("10 reps"))
        compose.onAllNodesWithText("10 reps").assertCountEquals(3)
        showWorkoutText("Front Plate Raise")
        showWorkoutText("8 reps")
        showWorkoutText("7.5 kg")
    }

    @Test fun exercisesFarBelowTheViewportAreRenderedOnDemand() {
        val exercises = List(40) { index -> exercise.copy(id = index.toLong(), name = "Exercise ${index + 1}") }
        compose.setContent { Content(WorkoutDetail(manual, exercises)) }

        compose.onNodeWithText("Exercise 40").assertDoesNotExist()
        showWorkoutText("Exercise 40")
        showWorkoutText("Add exercise")
    }

    @Test fun workoutTypeEditorShowsCurrentClassificationAndDispatchesSave() {
        val classification = WorkoutClassification(WorkoutType.Push, "Chest and triceps")
        var saved: WorkoutAction? = null
        compose.setContent {
            Content(WorkoutDetail(manual.copy(classification = classification), emptyList()), onAction = { saved = it })
        }

        compose.onNodeWithText("Push · Chest and triceps").assertIsDisplayed()
        compose.onNode(hasText("Edit workout") and hasClickAction()).performClick()
        compose.onNodeWithText("Workout type").assertIsDisplayed()
        compose.onNodeWithText("Chest and triceps").assertIsDisplayed()
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle { assertEquals(WorkoutAction.UpdateClassification(classification), saved) }
    }

    @Test fun bodyweightCardsShowOrderedRepsAndBodyWeightInTheWeightColumn() {
        val pushUps =
            exercise.copy(
                name = "Push-ups",
                catalog = catalog.copy(equipment = "other", muscles = listOf("chest")),
                sets = listOf(WorkoutSet(1, 15, null), WorkoutSet(2, 12, null), WorkoutSet(3, 10, null)),
            )
        compose.setContent { Content(WorkoutDetail(manual, listOf(pushUps))) }

        showWorkoutText("Chest")
        showWorkoutText("15 reps")
        showWorkoutText("12 reps")
        showWorkoutText("10 reps")
        assertTrue(compose.onAllNodesWithText("Body weight").fetchSemanticsNodes().size >= 3)
        compose.onNodeWithText("kg", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Other").assertDoesNotExist()
        screenshot("workout-bodyweight")
    }

    @Test fun mixedSetsKeepTheRecordedWeightIncludingZero() {
        val mixed = exercise.copy(sets = listOf(WorkoutSet(1, 10, null), WorkoutSet(2, 10, BigDecimal.ZERO)))
        compose.setContent { Content(WorkoutDetail(manual, listOf(mixed))) }

        showWorkoutText("0 kg")
        showWorkoutText("Body weight")
    }

    private fun showWorkoutText(text: String) {
        compose.onNodeWithTag("workout-list").performScrollToNode(hasText(text))
        val node = compose.onNodeWithText(text)
        compose.scrollToDisplayed(node)
        node.assertIsDisplayed()
    }

    @Composable private fun Content(
        detail: WorkoutDetail,
        dark: Boolean = false,
        onAction: (WorkoutAction) -> Unit = {},
    ) {
        PulseTheme(dark) {
            Box(Modifier.fillMaxSize().background(PulseTheme.colors.background)) {
                WorkoutScreen(
                    WorkoutUiState(detail = detail, loadState = FitnessLoadState.Ready),
                    onAction = onAction,
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
