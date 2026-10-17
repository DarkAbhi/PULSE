package dev.abhishekan.pulse.feature.fitness

import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.repository.FitnessRepository
import java.time.*
import kotlinx.coroutines.flow.MutableStateFlow

internal class FitnessTestRepository : FitnessRepository {
    override val changes = MutableStateFlow(0L)
    val today = LocalDate.of(2026, 10, 17)
    var overviewValue = FitnessOverview(today, emptyList(), emptyList())
    var detailValue = WorkoutDetail(WorkoutInfo(1, Instant.parse("2026-10-17T02:00:00Z"), null, null, null, null, false), emptyList())
    var readFailure: Failure? = null
    var overviewRead: (suspend () -> Outcome<FitnessOverview>)? = null
    var write: suspend () -> Outcome<Unit> = { Outcome.Success(Unit) }
    var records = 0; var saves = 0; var deletes = 0
    var selectedDate: LocalDate? = null
    var lastInput: ExerciseInput? = null
    var searchAction: suspend (String) -> Outcome<List<CatalogExercise>> = { Outcome.Success(emptyList()) }
    val trace = mutableListOf<String>()
    override suspend fun overview(): Outcome<FitnessOverview> {
        trace += "overview"
        return overviewRead?.invoke() ?: readFailure?.let { Outcome.Error(it) } ?: Outcome.Success(overviewValue)
    }
    override suspend fun workout(id: Long): Outcome<WorkoutDetail> {
        trace += "workout"
        return readFailure?.let { Outcome.Error(it) } ?: Outcome.Success(detailValue)
    }
    override suspend fun search(query: String) = searchAction(query)
    override suspend fun record(date: LocalDate?): Outcome<Long> {
        records++; selectedDate = date; trace += "record"
        return when (val result = write()) { is Outcome.Success -> Outcome.Success(2L); is Outcome.Error -> result }
    }
    override suspend fun saveExercise(visitId: Long, exerciseId: Long?, input: ExerciseInput): Outcome<Unit> {
        saves++; lastInput = input; trace += "save"; return write()
    }
    override suspend fun deleteWorkout(id: Long): Outcome<Unit> { deletes++; trace += "deleteWorkout"; return write() }
    override suspend fun deleteExercise(visitId: Long, exerciseId: Long): Outcome<Unit> { deletes++; trace += "deleteExercise"; return write() }
}
