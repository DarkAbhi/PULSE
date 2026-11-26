package dev.abhishekan.pulse.core.domain.repository
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.domain.model.ExerciseInput
import dev.abhishekan.pulse.core.domain.model.FitnessOverview
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.WorkoutClassification
import dev.abhishekan.pulse.core.domain.model.WorkoutDetail
import dev.abhishekan.pulse.core.domain.model.WorkoutScheduleDay
import kotlinx.coroutines.flow.StateFlow
import java.time.DayOfWeek
import java.time.LocalDate

interface FitnessRepository {
    val changes: StateFlow<Long>

    suspend fun overview(): Outcome<FitnessOverview>

    suspend fun workout(id: Long): Outcome<WorkoutDetail>

    suspend fun schedule(): Outcome<List<WorkoutScheduleDay>>

    suspend fun saveScheduleDay(
        day: DayOfWeek,
        classification: WorkoutClassification,
    ): Outcome<Unit>

    suspend fun updateClassification(
        id: Long,
        classification: WorkoutClassification,
    ): Outcome<Unit>

    suspend fun search(query: String): Outcome<List<CatalogExercise>>

    suspend fun record(date: LocalDate?): Outcome<Long>

    suspend fun saveExercise(
        visitId: Long,
        exerciseId: Long?,
        input: ExerciseInput,
    ): Outcome<Unit>

    suspend fun saveExercises(
        visitId: Long,
        inputs: List<ExerciseInput>,
    ): Outcome<Unit>

    suspend fun deleteWorkout(id: Long): Outcome<Unit>

    suspend fun deleteExercise(
        visitId: Long,
        exerciseId: Long,
    ): Outcome<Unit>
}
