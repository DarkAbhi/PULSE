package dev.abhishekan.pulse.core.domain.repository
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.domain.model.ExerciseInput
import dev.abhishekan.pulse.core.domain.model.FitnessOverview
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.WorkoutDetail
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

interface FitnessRepository {
    val changes: StateFlow<Long>

    suspend fun overview(): Outcome<FitnessOverview>

    suspend fun workout(id: Long): Outcome<WorkoutDetail>

    suspend fun search(query: String): Outcome<List<CatalogExercise>>

    suspend fun record(date: LocalDate?): Outcome<Long>

    suspend fun saveExercise(
        visitId: Long,
        exerciseId: Long?,
        input: ExerciseInput,
    ): Outcome<Unit>

    suspend fun deleteWorkout(id: Long): Outcome<Unit>

    suspend fun deleteExercise(
        visitId: Long,
        exerciseId: Long,
    ): Outcome<Unit>
}
