package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.repository.FitnessRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FitnessUseCasesTest {
    private class Repository : FitnessRepository {
        override val changes = MutableStateFlow(0L)
        var calls = 0
        var input: ExerciseInput? = null
        var date: LocalDate? = null
        override suspend fun overview(): Outcome<FitnessOverview> = error("Unused")
        override suspend fun workout(id: Long): Outcome<WorkoutDetail> = error("Unused")
        override suspend fun search(query: String): Outcome<List<CatalogExercise>> { calls++; return Outcome.Success(emptyList()) }
        override suspend fun record(date: LocalDate?): Outcome<Long> { calls++; this.date = date; return Outcome.Success(1L) }
        override suspend fun saveExercise(visitId: Long, exerciseId: Long?, input: ExerciseInput): Outcome<Unit> {
            calls++; this.input = input; return Outcome.Error(Failure.Server)
        }
        override suspend fun deleteWorkout(id: Long): Outcome<Unit> { calls++; return Outcome.Success(Unit) }
        override suspend fun deleteExercise(visitId: Long, exerciseId: Long): Outcome<Unit> { calls++; return Outcome.Success(Unit) }
    }
    @Test fun validationPreventsWritesAndSearchesBeforeRepository() = runTest {
        val repo = Repository()
        val invalid = Outcome.Error(Failure.InvalidInput)
        assertEquals(invalid, SaveExercise(repo)(1, null, ExerciseInput(" ", emptyList(), null, false)))
        assertEquals(invalid, DeleteWorkout(repo)(0))
        assertEquals(invalid, DeleteExercise(repo)(1, -1))
        assertEquals(invalid, SearchExercises(repo)(" "))
        assertEquals(invalid, RecordWorkout(repo)(LocalDate.of(10000, 1, 1)))
        assertEquals(0, repo.calls)
    }
    @Test fun preservesChosenCalendarDatesAndDoesNotInventPastOnlyRestriction() = runTest {
        val repo = Repository()
        val future = LocalDate.of(2090, 1, 2)
        assertEquals(Outcome.Success(1L), RecordWorkout(repo)(future))
        assertEquals(future, repo.date)
        RecordWorkout(repo)(null)
        assertNull(repo.date)
    }
    @Test fun trimsExerciseNameAndForwardsFailureWithoutRetrying() = runTest {
        val repo = Repository()
        assertEquals(Outcome.Error(Failure.Server), SaveExercise(repo)(1, 2, ExerciseInput(" Squat ", listOf(ExerciseSetInput(12, null)), null, false)))
        assertEquals("Squat", repo.input?.name)
        assertEquals(1, repo.calls)
    }
}
