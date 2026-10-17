package dev.abhishekan.pulse.feature.fitness

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.usecase.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private val repo = FitnessTestRepository()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { models.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }
    private fun vm(saved: SavedStateHandle = SavedStateHandle(mapOf("id" to 1L))) = WorkoutViewModel(GetWorkoutDetail(repo), SearchExercises(repo),
        SaveExercise(repo), DeleteWorkout(repo), DeleteExercise(repo), saved).also { models.put("workout", it) }
    private fun fill(vm: WorkoutViewModel) { vm.edit(); vm.nameChanged("Squat"); vm.setChanged(0, "10", "12.1234567890123456789") }

    @Test fun invalidInputNeverSavesAndSetLimitIsEnforced() = runTest {
        val vm = vm(); runCurrent(); vm.edit(); vm.save(); runCurrent()
        assertEquals(0, repo.saves)
        assertNotNull(vm.uiState.value.editor?.error)
        vm.nameChanged("Squat"); vm.setChanged(0, "1001", "-1"); vm.save(); runCurrent()
        assertEquals(0, repo.saves)
        repeat(25) { vm.addSet() }
        assertEquals(20, vm.uiState.value.editor?.draft?.sets?.size)
    }
    @Test fun duplicateSaveIsBlockedAndSuccessfulSavePreservesDecimalText() = runTest {
        val gate = CompletableDeferred<Outcome<Unit>>()
        repo.write = { gate.await() }
        val vm = vm(); runCurrent(); fill(vm)
        vm.save(); vm.save(); runCurrent()
        assertEquals(1, repo.saves)
        assertEquals("12.1234567890123456789", repo.lastInput?.sets?.single()?.weight?.toPlainString())
        gate.complete(Outcome.Success(Unit)); runCurrent()
        assertNull(vm.uiState.value.editor)
        assertEquals(listOf("workout", "save", "workout"), repo.trace)
    }
    @Test fun failedSaveKeepsInputAndRefreshesBeforeRetry() = runTest {
        repo.write = { Outcome.Error(Failure.Server) }
        val vm = vm(); runCurrent(); fill(vm); vm.save(); runCurrent()
        assertEquals("Squat", vm.uiState.value.editor?.draft?.name)
        assertTrue(vm.uiState.value.needsReview)
        assertTrue(vm.uiState.value.reviewReady)
        vm.save(); runCurrent(); assertEquals(1, repo.saves)
        assertEquals(listOf("workout", "save", "workout"), repo.trace)
        vm.acknowledgeReview(); assertFalse(vm.uiState.value.needsReview)
    }
    @Test fun savedDraftAndPendingMutationSurviveProcessStateRecreation() = runTest {
        val saved = SavedStateHandle(mapOf("id" to 1L))
        val first = vm(saved); runCurrent(); fill(first); first.addSet(); first.setChanged(1, "8", "")
        val gate = CompletableDeferred<Outcome<Unit>>()
        repo.write = { gate.await() }
        first.save(); runCurrent()
        val restored = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })
        val second = vm(restored); runCurrent()
        assertEquals("Squat", second.uiState.value.editor?.draft?.name)
        assertEquals(2, second.uiState.value.editor?.draft?.sets?.size)
        assertTrue(second.uiState.value.needsReview)
        assertTrue(second.uiState.value.reviewReady)
        second.save(); runCurrent(); assertEquals(1, repo.saves)
    }
    @Test fun deletesRefreshAndLostDeleteResponseRecognizesMissingWorkout() = runTest {
        val vm = vm(); runCurrent()
        vm.removeExercise(7); runCurrent()
        assertEquals(listOf("workout", "deleteExercise", "workout"), repo.trace)
        repo.write = { repo.readFailure = Failure.NotFound; Outcome.Error(Failure.Timeout) }
        vm.removeWorkout(); runCurrent()
        assertTrue(vm.uiState.value.deleted)
        assertEquals(2, repo.deletes)
    }
    @Test fun supersededCatalogueSearchCannotReplaceNewerResults() = runTest {
        val old = CompletableDeferred<Outcome<List<CatalogExercise>>>()
        repo.searchAction = { query -> if (query == "old") old.await() else Outcome.Success(listOf(CatalogExercise("new", "New match", null, emptyList(), emptyList()))) }
        val vm = vm(); runCurrent(); vm.edit(); vm.nameChanged("old"); advanceTimeBy(301); runCurrent()
        vm.nameChanged("new"); advanceTimeBy(301); runCurrent()
        old.complete(Outcome.Success(listOf(CatalogExercise("old", "Old match", null, emptyList(), emptyList())))); runCurrent()
        assertEquals("new", vm.uiState.value.editor?.candidates?.single()?.id)
    }
    @Test fun legacyExerciseWithoutSetsStartsWithAnEditableSet() = runTest {
        repo.detailValue = repo.detailValue.copy(exercises = listOf(WorkoutExercise(7, "Legacy exercise", null, null, emptyList())))
        val vm = vm(); runCurrent(); vm.edit(7)
        assertEquals(1, vm.uiState.value.editor?.draft?.sets?.size)
        vm.save(); runCurrent()
        assertEquals(0, repo.saves)
        assertNotNull(vm.uiState.value.editor?.error)
    }

    @Test fun offlineProcessRestorationKeepsDraftVisibleAndDirty() = runTest {
        val saved = SavedStateHandle(mapOf("id" to 1L))
        val first = vm(saved); runCurrent(); fill(first)
        val restored = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })
        repo.readFailure = Failure.Connection
        val second = vm(restored); runCurrent()
        assertEquals(Failure.Connection, second.uiState.value.error)
        assertEquals("Squat", second.uiState.value.editor?.draft?.name)
        assertTrue(second.uiState.value.editor?.dirty == true)
        assertEquals(0, repo.saves)
    }

}
