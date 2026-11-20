package dev.abhishekan.pulse.feature.fitness.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.WorkoutDetail
import dev.abhishekan.pulse.core.domain.model.WorkoutExercise
import dev.abhishekan.pulse.core.domain.usecase.DeleteExercise
import dev.abhishekan.pulse.core.domain.usecase.DeleteWorkout
import dev.abhishekan.pulse.core.domain.usecase.GetWorkoutDetail
import dev.abhishekan.pulse.core.domain.usecase.SaveExercise
import dev.abhishekan.pulse.core.domain.usecase.SearchExercises
import dev.abhishekan.pulse.feature.fitness.shared.FitnessTestRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private val repo = FitnessTestRepository()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun cleanup() {
        models.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    private fun vm(saved: SavedStateHandle = SavedStateHandle(mapOf("id" to 1L))) =
        WorkoutViewModel(
            GetWorkoutDetail(repo),
            SearchExercises(repo),
            SaveExercise(repo),
            DeleteWorkout(repo),
            DeleteExercise(repo),
            saved,
        ).also {
            models.put("workout", it)
        }

    private fun fill(vm: WorkoutViewModel) {
        vm.onAction(WorkoutAction.EditExercise())
        vm.onAction(WorkoutAction.ChangeName("Squat"))
        vm.onAction(WorkoutAction.ChangeSet(0, "10", "12.1234567890123456789"))
    }

    @Test fun firstResumeDoesNotRepeatCompletedInitialLoadAndLaterResumeRetainsDetail() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.onAction(WorkoutAction.Resume)
            runCurrent()
            assertEquals(listOf("workout"), repo.trace)
            val detail = vm.uiState.value.detail
            val gate = CompletableDeferred<Outcome<WorkoutDetail>>()
            repo.detailRead = { gate.await() }
            vm.onAction(WorkoutAction.Resume)
            runCurrent()
            assertTrue(vm.uiState.value.loading)
            assertEquals(detail, vm.uiState.value.detail)
            vm.onAction(WorkoutAction.Refresh)
            runCurrent()
            assertEquals(listOf("workout", "workout"), repo.trace)
            gate.complete(Outcome.Error(Failure.Connection))
            runCurrent()
            assertFalse(vm.uiState.value.loading)
            assertEquals(detail, vm.uiState.value.detail)
            assertEquals(Failure.Connection, vm.uiState.value.error)
        }

    @Test fun firstResumeDuringInitialLoadDoesNotQueueAnotherRead() =
        runTest {
            val gate = CompletableDeferred<Outcome<WorkoutDetail>>()
            repo.detailRead = { gate.await() }
            val vm = vm()
            vm.onAction(WorkoutAction.Resume)
            runCurrent()
            gate.complete(Outcome.Success(repo.detailValue))
            runCurrent()
            assertEquals(listOf("workout"), repo.trace)
        }

    @Test fun refreshPreservesUnsavedExerciseDraft() =
        runTest {
            val vm = vm()
            runCurrent()
            fill(vm)
            val draft =
                vm.uiState.value.editor
                    ?.draft
            val gate = CompletableDeferred<Outcome<WorkoutDetail>>()
            repo.detailRead = { gate.await() }
            vm.onAction(WorkoutAction.Refresh)
            runCurrent()
            assertEquals(
                draft,
                vm.uiState.value.editor
                    ?.draft,
            )
            gate.complete(Outcome.Success(repo.detailValue))
            runCurrent()
            assertEquals(
                draft,
                vm.uiState.value.editor
                    ?.draft,
            )
        }

    @Test fun editorKeepsItsOriginalBaselineWhenServerExerciseChanges() =
        runTest {
            val exercise = WorkoutExercise(7, "Squat", null, null, emptyList())
            repo.detailValue = repo.detailValue.copy(exercises = listOf(exercise))
            val vm = vm()
            runCurrent()
            vm.onAction(WorkoutAction.EditExercise(7))
            vm.onAction(WorkoutAction.ChangeName("Front squat"))

            repo.detailValue = repo.detailValue.copy(exercises = listOf(exercise.copy(name = "Front squat")))
            vm.onAction(WorkoutAction.Refresh)
            runCurrent()
            vm.onAction(WorkoutAction.ChangeName("Squat"))

            assertFalse(checkNotNull(vm.uiState.value.editor).dirty)
        }

    @Test fun staleWorkoutActionsAfterFailedRefreshCannotOpenEditorOrDelete() =
        runTest {
            val vm = vm()
            runCurrent()
            repo.readFailure = Failure.Connection
            vm.onAction(WorkoutAction.Refresh)
            runCurrent()

            vm.onAction(WorkoutAction.EditExercise())
            vm.onAction(WorkoutAction.DeleteWorkout)
            vm.onAction(WorkoutAction.DeleteExercise(7))
            runCurrent()

            assertNotNull(vm.uiState.value.detail)
            assertNull(vm.uiState.value.editor)
            assertEquals(0, repo.deletes)
        }

    @Test fun invalidInputNeverSavesAndSetLimitIsEnforced() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.onAction(WorkoutAction.EditExercise())
            vm.onAction(WorkoutAction.SaveExercise)
            runCurrent()
            assertEquals(0, repo.saves)
            assertNotNull(
                vm.uiState.value.editor
                    ?.error,
            )
            vm.onAction(WorkoutAction.ChangeName("Squat"))
            vm.onAction(WorkoutAction.ChangeSet(0, "1001", "-1"))
            vm.onAction(WorkoutAction.SaveExercise)
            runCurrent()
            assertEquals(0, repo.saves)
            repeat(25) { vm.onAction(WorkoutAction.AddSet) }
            assertEquals(
                20,
                vm.uiState.value.editor
                    ?.draft
                    ?.sets
                    ?.size,
            )
        }

    @Test fun duplicateSaveIsBlockedAndSuccessfulSavePreservesDecimalText() =
        runTest {
            val gate = CompletableDeferred<Outcome<Unit>>()
            repo.write = { gate.await() }
            val vm = vm()
            runCurrent()
            fill(vm)
            vm.onAction(WorkoutAction.SaveExercise)
            vm.onAction(WorkoutAction.SaveExercise)
            runCurrent()
            assertEquals(1, repo.saves)
            assertEquals(
                "12.1234567890123456789",
                repo.lastInput
                    ?.sets
                    ?.single()
                    ?.weight
                    ?.toPlainString(),
            )
            gate.complete(Outcome.Success(Unit))
            runCurrent()
            assertNull(vm.uiState.value.editor)
            assertEquals(listOf("workout", "save", "workout"), repo.trace)
        }

    @Test fun failedSaveKeepsInputAndRefreshesBeforeRetry() =
        runTest {
            repo.write = { Outcome.Error(Failure.Server) }
            val vm = vm()
            runCurrent()
            fill(vm)
            vm.onAction(WorkoutAction.SaveExercise)
            runCurrent()
            assertEquals(
                "Squat",
                vm.uiState.value.editor
                    ?.draft
                    ?.name,
            )
            assertTrue(vm.uiState.value.needsReview)
            assertTrue(vm.uiState.value.reviewReady)
            vm.onAction(WorkoutAction.SaveExercise)
            runCurrent()
            assertEquals(1, repo.saves)
            assertEquals(listOf("workout", "save", "workout"), repo.trace)
            vm.onAction(WorkoutAction.AcknowledgeReview)
            assertFalse(vm.uiState.value.needsReview)
        }

    @Test fun savedDraftAndPendingMutationSurviveProcessStateRecreation() =
        runTest {
            val saved = SavedStateHandle(mapOf("id" to 1L))
            val first = vm(saved)
            runCurrent()
            fill(first)
            first.onAction(WorkoutAction.AddSet)
            first.onAction(WorkoutAction.ChangeSet(1, "8", ""))
            val gate = CompletableDeferred<Outcome<Unit>>()
            repo.write = { gate.await() }
            first.onAction(WorkoutAction.SaveExercise)
            runCurrent()
            val restored = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })
            val second = vm(restored)
            runCurrent()
            assertEquals(
                "Squat",
                second.uiState.value.editor
                    ?.draft
                    ?.name,
            )
            assertEquals(
                2,
                second.uiState.value.editor
                    ?.draft
                    ?.sets
                    ?.size,
            )
            assertTrue(second.uiState.value.needsReview)
            assertTrue(second.uiState.value.reviewReady)
            second.onAction(WorkoutAction.SaveExercise)
            runCurrent()
            assertEquals(1, repo.saves)
        }

    @Test fun deletesRefreshAndLostDeleteResponseRecognizesMissingWorkout() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.onAction(WorkoutAction.DeleteExercise(7))
            runCurrent()
            assertEquals(listOf("workout", "deleteExercise", "workout"), repo.trace)
            repo.write = {
                repo.readFailure = Failure.NotFound
                Outcome.Error(Failure.Timeout)
            }
            vm.onAction(WorkoutAction.DeleteWorkout)
            runCurrent()
            assertTrue(vm.uiState.value.deleted)
            assertEquals(2, repo.deletes)
        }

    @Test fun changingCatalogueQueryClearsPreviousChoicesBeforeSearching() =
        runTest {
            repo.searchAction = { query ->
                Outcome.Success(listOf(CatalogExercise(query, query, null, emptyList(), emptyList())))
            }
            val vm = vm()
            runCurrent()
            vm.onAction(WorkoutAction.EditExercise())
            vm.onAction(WorkoutAction.SearchCatalogue("first"))
            advanceTimeBy(301)
            assertEquals(
                "first",
                vm.uiState.value.editor
                    ?.candidates
                    ?.single()
                    ?.id,
            )

            vm.onAction(WorkoutAction.SearchCatalogue("second"))
            assertEquals(
                emptyList<CatalogExercise>(),
                vm.uiState.value.editor
                    ?.candidates,
            )
            assertTrue(
                vm.uiState.value.editor
                    ?.searching == true,
            )
            advanceTimeBy(301)
            assertEquals(
                "second",
                vm.uiState.value.editor
                    ?.candidates
                    ?.single()
                    ?.id,
            )
        }

    @Test fun supersededCatalogueSearchCannotReplaceNewerResults() =
        runTest {
            val old = CompletableDeferred<Outcome<List<CatalogExercise>>>()
            repo.searchAction =
                { query ->
                    if (query ==
                        "old"
                    ) {
                        old.await()
                    } else {
                        Outcome.Success(
                            listOf(CatalogExercise("new", "New match", null, emptyList(), emptyList())),
                        )
                    }
                }
            val vm = vm()
            runCurrent()
            vm.onAction(WorkoutAction.EditExercise())
            vm.onAction(WorkoutAction.ChangeName("old"))
            advanceTimeBy(301)
            runCurrent()
            vm.onAction(WorkoutAction.ChangeName("new"))
            advanceTimeBy(301)
            runCurrent()
            old.complete(
                Outcome.Success(
                    listOf(CatalogExercise("old", "Old match", null, emptyList(), emptyList())),
                ),
            )
            runCurrent()
            assertEquals(
                "new",
                vm.uiState.value.editor
                    ?.candidates
                    ?.single()
                    ?.id,
            )
        }

    @Test fun legacyExerciseWithoutSetsStartsWithAnEditableSet() =
        runTest {
            repo.detailValue =
                repo.detailValue.copy(
                    exercises = listOf(WorkoutExercise(7, "Legacy exercise", null, null, emptyList())),
                )
            val vm = vm()
            runCurrent()
            vm.onAction(WorkoutAction.EditExercise(7))
            assertEquals(
                1,
                vm.uiState.value.editor
                    ?.draft
                    ?.sets
                    ?.size,
            )
            vm.onAction(WorkoutAction.SaveExercise)
            runCurrent()
            assertEquals(0, repo.saves)
            assertNotNull(
                vm.uiState.value.editor
                    ?.error,
            )
        }

    @Test fun offlineProcessRestorationKeepsDraftVisibleAndDirty() =
        runTest {
            val saved = SavedStateHandle(mapOf("id" to 1L))
            val first = vm(saved)
            runCurrent()
            fill(first)
            val restored = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })
            repo.readFailure = Failure.Connection
            val second = vm(restored)
            runCurrent()
            assertEquals(Failure.Connection, second.uiState.value.error)
            assertEquals(
                "Squat",
                second.uiState.value.editor
                    ?.draft
                    ?.name,
            )
            assertTrue(
                second.uiState.value.editor
                    ?.dirty == true,
            )
            assertEquals(0, repo.saves)
        }

    @Test fun browsingReplacementPreservesNameAliasMatchAndRawSets() =
        runTest {
            val candidate = CatalogExercise("curl", "Bicep curl", "dumbbell", listOf("biceps"), listOf("Curl with control."))
            repo.searchAction = { Outcome.Success(listOf(candidate.copy(id = "other", name = "Other exercise"))) }
            val vm = vm()
            runCurrent()
            vm.onAction(WorkoutAction.EditExercise())
            vm.onAction(WorkoutAction.ChooseMatch(candidate))
            vm.onAction(WorkoutAction.ChangeName("My curls"))
            vm.onAction(WorkoutAction.RememberAlias(true))
            vm.onAction(WorkoutAction.ChangeSet(0, "12", "12.1234567890123456789"))
            vm.onAction(WorkoutAction.AddSet)
            vm.onAction(WorkoutAction.ChangeSet(1, "", "invalid."))
            val before =
                vm.uiState.value.editor!!
                    .draft
            vm.onAction(WorkoutAction.SearchCatalogue("Other exercise"))
            advanceTimeBy(301)
            runCurrent()
            assertEquals(
                before,
                vm.uiState.value.editor!!
                    .draft,
            )
            assertEquals(
                candidate,
                vm.uiState.value.editor!!
                    .selectedCatalog,
            )
            vm.onAction(WorkoutAction.ChooseMatch(candidate.copy(id = "other", name = "Other exercise")))
            assertEquals(
                before.sets,
                vm.uiState.value.editor!!
                    .draft.sets,
            )
            assertEquals(
                "other",
                vm.uiState.value.editor!!
                    .draft.catalogId,
            )
            assertFalse(
                vm.uiState.value.editor!!
                    .draft.rememberAlias,
            )
        }

    @Test fun replacementSearchRetainsSelectedMetadataAcrossRefreshAndErrors() =
        runTest {
            val candidate = CatalogExercise("curl", "Bicep curl", null, emptyList(), listOf("Curl with control."))
            repo.searchAction = { Outcome.Error(Failure.Connection) }
            val vm = vm()
            runCurrent()
            vm.onAction(WorkoutAction.EditExercise())
            vm.onAction(WorkoutAction.ChooseMatch(candidate))
            vm.onAction(WorkoutAction.SearchCatalogue("unavailable"))
            advanceTimeBy(301)
            runCurrent()
            assertNotNull(
                vm.uiState.value.editor!!
                    .searchError,
            )
            assertEquals(
                candidate,
                vm.uiState.value.editor!!
                    .selectedCatalog,
            )
            vm.onAction(WorkoutAction.Refresh)
            runCurrent()
            assertEquals(
                candidate,
                vm.uiState.value.editor!!
                    .selectedCatalog,
            )
            assertEquals(
                candidate.id,
                vm.uiState.value.editor!!
                    .draft.catalogId,
            )
        }
}
