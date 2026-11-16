package dev.abhishekan.pulse

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.feature.fitness.shared.FitnessLoadState
import dev.abhishekan.pulse.feature.fitness.workout.editor.ExerciseDraft
import dev.abhishekan.pulse.feature.fitness.workout.editor.ExerciseEditorScreen
import dev.abhishekan.pulse.feature.fitness.workout.editor.ExerciseEditorState
import dev.abhishekan.pulse.feature.fitness.workout.editor.SetDraft
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutAction
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutUiState
import org.junit.Rule
import org.junit.Test
import java.io.File

class ExerciseEditorRedesignTest {
    @get:Rule val compose = createComposeRule()
    private val catalogue = CatalogExercise("curl", "Dumbbell curl", "Dumbbell", listOf("Biceps"), listOf("Curl with control."))
    private val editor = mutableStateOf(ExerciseEditorState(ExerciseDraft()))
    private var saved = false
    private var cancelled = false

    private fun show(
        initial: ExerciseEditorState = ExerciseEditorState(ExerciseDraft()),
        largeType: Boolean = false,
    ) {
        editor.value = initial
        compose.setContent {
            PulseTheme(darkTheme = largeType) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeType) 1.6f else density.fontScale)) {
                    Box(Modifier.fillMaxSize().safeDrawingPadding().background(PulseTheme.colors.background)) {
                        ExerciseEditorScreen(
                            editor = editor.value,
                            state = WorkoutUiState(loadState = FitnessLoadState.Ready),
                            onAction = { action ->
                                val current = editor.value
                                val draft = current.draft
                                when (action) {
                                    is WorkoutAction.ChangeName -> editor.value = current.copy(draft = draft.copy(name = action.name))
                                    is WorkoutAction.ChangeSet ->
                                        editor.value = current.copy(
                                            draft = draft.copy(
                                                sets = draft.sets.mapIndexed { index, set ->
                                                    if (index == action.index) SetDraft(action.reps ?: set.reps, action.weight ?: set.weight) else set
                                                },
                                            ),
                                        )
                                    WorkoutAction.AddSet -> editor.value = current.copy(draft = draft.copy(sets = draft.sets + SetDraft()))
                                    is WorkoutAction.RemoveSet ->
                                        editor.value = current.copy(draft = draft.copy(sets = draft.sets.filterIndexed { index, _ -> index != action.index }))
                                    is WorkoutAction.ChooseMatch ->
                                        editor.value = current.copy(
                                            draft = draft.copy(name = action.candidate.name, catalogId = action.candidate.id),
                                            selectedCatalog = action.candidate,
                                        )
                                    WorkoutAction.ClearMatch ->
                                        editor.value = current.copy(draft = draft.copy(catalogId = null, rememberAlias = false))
                                    is WorkoutAction.SearchCatalogue ->
                                        editor.value = current.copy(candidates = if (action.query.isBlank()) emptyList() else listOf(catalogue))
                                    is WorkoutAction.RememberAlias -> editor.value = current.copy(draft = draft.copy(rememberAlias = action.value))
                                    WorkoutAction.SaveExercise -> saved = true
                                    else -> Unit
                                }
                            },
                            onCancel = { cancelled = true },
                        )
                    }
                }
            }
        }
    }

    @Test fun initialStateAndCustomNameFlow() {
        show()
        compose.onNodeWithText("Find an exercise to get started").assertIsDisplayed()
        compose.onNodeWithText("Your sets").assertDoesNotExist()
        screenshot("empty")
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithText("Search for an exercise").performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText("Search for an exercise")).assertIsFocused()
        compose.onNode(hasSetTextAction() and hasText("Search for an exercise")).performTextInput("My custom exercise")
        compose.onNodeWithText("Use My custom exercise as a custom exercise").performClick()
        compose.onNodeWithText("Custom exercise").assertExists()
        compose.onNodeWithText("Reps, set 1").performScrollTo().performTextInput("12")
        compose
            .onNodeWithText("Save")
            .assertIsEnabled()
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle {
            check(saved)
            check(editor.value.draft.catalogId == null)
        }
    }

    @Test fun replacementAndInstructionsDismissalRetainRawSets() {
        val draft =
            ExerciseDraft(
                name = "Dumbbell curl",
                catalogId = "curl",
                sets = listOf(SetDraft("12", "12.123456789"), SetDraft("", "invalid.")),
            )
        show(ExerciseEditorState(draft, selectedCatalog = catalogue))
        compose.onNodeWithText("Change").performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText("Search for an exercise")).performTextReplacement("Another exercise")
        compose.onNodeWithContentDescription("Close sheet").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle { check(editor.value.draft == draft) }
        compose.onNodeWithText("View instructions").performScrollTo().performClick()
        compose.onNodeWithText("1. Curl with control.").assertIsDisplayed()
        screenshot("instructions")
        compose.onNodeWithContentDescription("Close sheet").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle { check(editor.value.draft == draft) }
        compose.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test fun immediateCatalogueSelectionAndSetControls() {
        show()
        compose.onNodeWithText("Search for an exercise").performClick()
        compose.onNode(hasSetTextAction() and hasText("Search for an exercise")).performTextInput("curl")
        compose.onNodeWithText("Dumbbell • Biceps").assertIsDisplayed()
        screenshot("search")
        compose.onNodeWithText("Dumbbell curl").performClick()
        compose.onNodeWithText("Choose an exercise").assertDoesNotExist()
        screenshot("selected")
        compose.onNodeWithText("Add another set").performScrollTo().performClick()
        compose.runOnIdle { check(editor.value.draft.sets.size == 2) }
        compose.onNodeWithContentDescription("Remove set 1").performScrollTo().performClick()
        compose.runOnIdle { check(editor.value.draft.sets.size == 1) }
        compose.onNodeWithText("Remember this name for future workouts").performScrollTo().performClick()
        compose.runOnIdle { check(editor.value.draft.rememberAlias) }
        compose.onNodeWithContentDescription("Back").assertDoesNotExist()
    }

    @Test fun maximumSetsAndInvalidRepetitionsKeepActionsDisabled() {
        show(ExerciseEditorState(ExerciseDraft(name = "Custom", sets = List(20) { SetDraft("1001", "") })))
        compose.onNodeWithText("Add another set").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test fun largeTypeAndDarkThemeKeepSetFieldsAndInstructionsAccessible() {
        show(
            ExerciseEditorState(
                ExerciseDraft(name = catalogue.name, catalogId = catalogue.id, sets = listOf(SetDraft("12", "10"))),
                selectedCatalog = catalogue,
            ),
            largeType = true,
        )
        compose.onNodeWithText("Weight (kg), set 1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Save").assertIsEnabled()
        screenshot("large-type-dark")
        compose.onNodeWithText("View instructions").performScrollTo().performClick()
        compose.onNodeWithText("1. Curl with control.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close sheet").performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "exercise-editor-redesign")
        directory.mkdirs()
        SystemClock.sleep(250)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
