package dev.abhishekan.pulse

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.feature.fitness.shared.FitnessLoadState
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutAction
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutUiState
import dev.abhishekan.pulse.feature.fitness.workout.editor.ExerciseDraft
import dev.abhishekan.pulse.feature.fitness.workout.editor.ExerciseEditorScreen
import dev.abhishekan.pulse.feature.fitness.workout.editor.ExerciseEditorState
import dev.abhishekan.pulse.feature.fitness.workout.editor.SetDraft
import org.junit.Rule
import org.junit.Test
import java.io.File

class ExerciseEditorRedesignTest {
    @get:Rule val compose = createComposeRule()
    private val catalogue = CatalogExercise("curl", "Dumbbell curl", "Dumbbell", listOf("Biceps"), listOf("Curl with control."))
    private val editor = mutableStateOf(ExerciseEditorState(ExerciseDraft()))
    private val viewport = mutableStateOf(Dp.Unspecified)
    private var saved = false
    private var cancelled = false

    private fun show(
        initial: ExerciseEditorState = ExerciseEditorState(ExerciseDraft()),
        largeType: Boolean = false,
        viewportWidth: Dp = Dp.Unspecified,
        darkTheme: Boolean = largeType,
    ) {
        editor.value = initial
        viewport.value = viewportWidth
        compose.setContent {
            PulseTheme(darkTheme = darkTheme) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeType) 1.6f else 1f)) {
                    Box(
                        Modifier
                            .widthIn(max = viewport.value)
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .background(PulseTheme.colors.background),
                    ) {
                        ExerciseEditorScreen(
                            editor = editor.value,
                            state = WorkoutUiState(loadState = FitnessLoadState.Ready),
                            onAction = { action ->
                                val current = editor.value
                                val draft = current.draft
                                when (action) {
                                    is WorkoutAction.ChangeName -> editor.value = current.copy(draft = draft.copy(name = action.name))
                                    is WorkoutAction.ChangeSet ->
                                        editor.value =
                                            current.copy(
                                                draft =
                                                    draft.copy(
                                                        sets =
                                                            draft.sets.mapIndexed { index, set ->
                                                                if (index ==
                                                                    action.index
                                                                ) {
                                                                    SetDraft(action.reps ?: set.reps, action.weight ?: set.weight)
                                                                } else {
                                                                    set
                                                                }
                                                            },
                                                    ),
                                            )
                                    WorkoutAction.AddSet -> editor.value = current.copy(draft = draft.copy(sets = draft.sets + SetDraft()))
                                    is WorkoutAction.RemoveSet ->
                                        editor.value =
                                            current.copy(
                                                draft =
                                                    draft.copy(
                                                        sets =
                                                            draft.sets.filterIndexed { index, _ ->
                                                                index !=
                                                                    action.index
                                                            },
                                                    ),
                                            )
                                    is WorkoutAction.ChooseMatch ->
                                        editor.value =
                                            current.copy(
                                                draft = draft.copy(name = action.candidate.name, catalogId = action.candidate.id),
                                                selectedCatalog = action.candidate,
                                            )
                                    WorkoutAction.ClearMatch ->
                                        editor.value = current.copy(draft = draft.copy(catalogId = null, rememberAlias = false))
                                    is WorkoutAction.SearchCatalogue ->
                                        editor.value =
                                            current.copy(candidates = if (action.query.isBlank()) emptyList() else listOf(catalogue))
                                    is WorkoutAction.RememberAlias ->
                                        editor.value =
                                            current.copy(draft = draft.copy(rememberAlias = action.value))
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
        compose.onNodeWithText("Sets").assertDoesNotExist()
        screenshot("empty")
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithText("Search for an exercise").performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText("Search for an exercise")).assertIsFocused()
        compose.onNode(hasSetTextAction() and hasText("Search for an exercise")).performTextInput("My custom exercise")
        compose.onNodeWithText("Use My custom exercise as a custom exercise").performClick()
        compose.onNodeWithText("Custom exercise").assertExists()
        compose.onNodeWithContentDescription("Reps, set 1").performScrollTo().performTextInput("12")
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
        compose.onNodeWithText("How to do it").performScrollTo().performClick()
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
        compose.onNodeWithText("Remember my name for this exercise").performScrollTo().performClick()
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
                ExerciseDraft(name = catalogue.name, catalogId = catalogue.id, sets = listOf(SetDraft("12", "10"), SetDraft("8", ""))),
                selectedCatalog = catalogue,
            ),
            largeType = true,
            viewportWidth = 320.dp,
        )
        val reps = compose.onNodeWithContentDescription("Reps, set 1")
        val weight = compose.onNodeWithContentDescription("Weight (kg), set 1")
        weight.performScrollTo().assertIsDisplayed()
        check(weight.getUnclippedBoundsInRoot().top > reps.getUnclippedBoundsInRoot().bottom)
        compose.onNodeWithText("Save").assertIsEnabled()
        screenshot("large-type-dark")
        compose.onNodeWithText("How to do it").performScrollTo().performClick()
        compose.onNodeWithText("1. Curl with control.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close sheet").performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    @Test fun narrowViewportKeepsSharedHeadingsAndKeyboardOrder() {
        show(
            ExerciseEditorState(
                ExerciseDraft(
                    exerciseId = 1,
                    name = "My dumbbell curl",
                    catalogId = catalogue.id,
                    sets = listOf(SetDraft("12", "12.5"), SetDraft("8", ""), SetDraft("12", "10")),
                ),
                selectedCatalog = catalogue,
            ),
            viewportWidth = 320.dp,
            darkTheme = true,
        )
        val actionLabelLayout = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText("How to do it", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(actionLabelLayout) }
        check(actionLabelLayout.single().lineCount == 1)
        compose.onNodeWithText("Reps").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Weight (kg)").assertIsDisplayed()
        val reps = compose.onNodeWithContentDescription("Reps, set 1")
        val weight = compose.onNodeWithContentDescription("Weight (kg), set 1")
        reps.performScrollTo().assertIsDisplayed()
        weight.assertIsDisplayed()
        check(reps.getUnclippedBoundsInRoot().top == weight.getUnclippedBoundsInRoot().top)
        screenshot("narrow")
        reps.performTextReplacement("1000")
        reps.performImeAction()
        weight.assertIsFocused().performImeAction()
        compose.onNodeWithContentDescription("Reps, set 2").assertIsFocused()
        compose.runOnIdle {
            check(
                editor.value.draft.sets
                    .first() == SetDraft("1000", "12.5"),
            )
        }
    }

    @Test fun instructionsStayOpenAcrossLayoutBreakpoint() {
        show(
            ExerciseEditorState(ExerciseDraft(name = catalogue.name, catalogId = catalogue.id), selectedCatalog = catalogue),
            viewportWidth = 320.dp,
        )
        val change = compose.onNodeWithText("Change")
        val instructions = compose.onNodeWithText("How to do it")
        check(change.getUnclippedBoundsInRoot().top == instructions.getUnclippedBoundsInRoot().top)
        instructions.performScrollTo().performClick()
        val instruction = compose.onNodeWithText("1. Curl with control.")
        instruction.assertIsDisplayed()

        compose.runOnIdle { viewport.value = 248.dp }
        instruction.assertIsDisplayed()
        compose.onNodeWithContentDescription("Close sheet").performSemanticsAction(SemanticsActions.OnClick) { it() }
        instruction.assertDoesNotExist()
        check(instructions.getUnclippedBoundsInRoot().top > change.getUnclippedBoundsInRoot().top)
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
