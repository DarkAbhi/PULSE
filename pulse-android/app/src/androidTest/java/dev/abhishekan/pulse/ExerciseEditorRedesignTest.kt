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
import dev.abhishekan.pulse.feature.fitness.ExerciseDraft
import dev.abhishekan.pulse.feature.fitness.ExerciseEditorState
import dev.abhishekan.pulse.feature.fitness.SetDraft
import dev.abhishekan.pulse.feature.fitness.WorkoutScreen
import dev.abhishekan.pulse.feature.fitness.WorkoutUiState
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
                        WorkoutScreen(
                            WorkoutUiState(editor = editor.value, loading = false),
                            onRefresh = {},
                            onEdit = {},
                            onDelete = {},
                            onDeleteExercise = {},
                            onReviewed = {},
                            onName = { editor.value = editor.value.copy(draft = editor.value.draft.copy(name = it)) },
                            onSet = { index, reps, weight ->
                                editor.value =
                                    editor.value.copy(
                                        draft =
                                            editor.value.draft.copy(
                                                sets =
                                                    editor.value.draft.sets.mapIndexed { i, set ->
                                                        if (i == index) SetDraft(reps ?: set.reps, weight ?: set.weight) else set
                                                    },
                                            ),
                                    )
                            },
                            onAddSet = {
                                editor.value =
                                    editor.value.copy(draft = editor.value.draft.copy(sets = editor.value.draft.sets + SetDraft()))
                            },
                            onRemoveSet = { index ->
                                editor.value =
                                    editor.value.copy(
                                        draft =
                                            editor.value.draft.copy(
                                                sets =
                                                    editor.value.draft.sets.filterIndexed { i, _ ->
                                                        i !=
                                                            index
                                                    },
                                            ),
                                    )
                            },
                            onChoose = {
                                editor.value =
                                    editor.value.copy(
                                        draft = editor.value.draft.copy(name = it.name, catalogId = it.id),
                                        selectedCatalog = it,
                                    )
                            },
                            onClearMatch = {
                                editor.value =
                                    editor.value.copy(
                                        draft = editor.value.draft.copy(catalogId = null, rememberAlias = false),
                                    )
                            },
                            onSearch = {},
                            onSearchQuery = {
                                editor.value =
                                    editor.value.copy(candidates = if (it.isBlank()) emptyList() else listOf(catalogue))
                            },
                            onAlias = { editor.value = editor.value.copy(draft = editor.value.draft.copy(rememberAlias = it)) },
                            onSave = { saved = true },
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
        compose.onNodeWithText("Save exercise").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Search for an exercise…").performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText("Search for an exercise…")).performTextInput("My custom exercise")
        compose.onNodeWithText("Use My custom exercise as a custom exercise").performClick()
        compose.onNodeWithText("Custom exercise").assertExists()
        compose.onNodeWithText("Reps, set 1").performScrollTo().performTextInput("12")
        compose
            .onNodeWithText("Save exercise")
            .performScrollTo()
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
        compose.onNode(hasSetTextAction() and hasText("Search for an exercise…")).performTextReplacement("Another exercise")
        compose.onNodeWithText("Close").performClick()
        compose.runOnIdle { check(editor.value.draft == draft) }
        compose.onNodeWithText("View instructions").performScrollTo().performClick()
        compose.onNodeWithText("1. Curl with control.").assertIsDisplayed()
        screenshot("instructions")
        compose.onNodeWithText("Close").performClick()
        compose.runOnIdle { check(editor.value.draft == draft) }
        compose.onNodeWithText("Save exercise").performScrollTo().assertIsNotEnabled()
    }

    @Test fun immediateCatalogueSelectionAndSetControls() {
        show()
        compose.onNodeWithText("Search for an exercise…").performClick()
        compose.onNode(hasSetTextAction() and hasText("Search for an exercise…")).performTextInput("curl")
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
        compose.onNodeWithText("Save exercise").performScrollTo().assertIsNotEnabled()
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
        compose.onNodeWithText("Save exercise").performScrollTo().assertIsEnabled()
        screenshot("large-type-dark")
        compose.onNodeWithText("View instructions").performScrollTo().performClick()
        compose.onNodeWithText("1. Curl with control.").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
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
