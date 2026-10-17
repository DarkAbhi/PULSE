package dev.abhishekan.pulse.core.data
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.domain.model.ExerciseInput
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.FitnessDates
import dev.abhishekan.pulse.core.domain.model.FitnessMetric
import dev.abhishekan.pulse.core.domain.model.FitnessOverview
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.WorkoutDetail
import dev.abhishekan.pulse.core.domain.model.WorkoutExercise
import dev.abhishekan.pulse.core.domain.model.WorkoutInfo
import dev.abhishekan.pulse.core.domain.model.WorkoutSet
import dev.abhishekan.pulse.core.domain.model.WorkoutSummary
import dev.abhishekan.pulse.core.domain.repository.FitnessRepository
import dev.abhishekan.pulse.core.network.FitnessApi
import dev.abhishekan.pulse.core.network.dto.CatalogExerciseDto
import dev.abhishekan.pulse.core.network.dto.ExerciseRequest
import dev.abhishekan.pulse.core.network.dto.ExerciseSetRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PulseFitnessRepository(
    private val api: FitnessApi,
    private val auth: PulseAuthRepository,
) : FitnessRepository {
    private val revision = MutableStateFlow(0L)
    override val changes = revision.asStateFlow()

    private suspend fun <T> request(
        mutation: Boolean = false,
        block: suspend () -> T,
    ): Outcome<T> {
        val version = auth.version
        return try {
            val result = withContext(Dispatchers.Default) { block() }
            if (mutation) revision.update { it + 1 }
            Outcome.Success(result)
        } catch (error: Exception) {
            val failure = remoteFailure(error)
            if (failure == Failure.Unauthorized) auth.expire(version)
            // A lost response can follow a committed write. Observers must revalidate even on failure.
            if (mutation) revision.update { it + 1 }
            Outcome.Error(failure)
        }
    }

    override suspend fun overview(): Outcome<FitnessOverview> =
        request {
            coroutineScope {
                val overview = async { api.overview() }
                val visits = async { api.visits() }
                val dto = overview.await()
                val summaries =
                    dto.workouts
                        .orEmpty()
                        .flatMap { (date, group) ->
                            require(LocalDate.parse(date) == LocalDate.parse(group.date))
                            group.workouts.orEmpty().map {
                                require(it.id > 0)
                                WorkoutSummary(it.id, LocalDate.parse(date), it.timeLabel)
                            }
                        }.associateBy { it.id }
                        .toMutableMap()
                val history = visits.await()
                summaries.keys.retainAll(history.map { it.id }.toSet())
                history.forEach {
                    require(it.id > 0)
                    val time = Instant.parse(it.createdAt).atZone(FitnessDates.zone)
                    summaries[it.id] =
                        WorkoutSummary(
                            it.id,
                            time.toLocalDate(),
                            summaries[it.id]?.timeLabel
                                ?: time.format(DateTimeFormatter.ofPattern("h:mm a")),
                        )
                }
                FitnessOverview(
                    LocalDate.parse(dto.today),
                    dto.cards.orEmpty().map {
                        FitnessMetric(
                            it.id,
                            it.label,
                            it.value,
                            it.subtitle,
                            it.tooltip?.takeIf(String::isNotBlank),
                        )
                    },
                    history.map { checkNotNull(summaries[it.id]) },
                )
            }
        }

    override suspend fun workout(id: Long): Outcome<WorkoutDetail> =
        request {
            coroutineScope {
                val info = async { api.workout(id) }
                val exercises = async { api.exercises(id) }
                val dto = info.await()
                require(dto.id == id)
                WorkoutDetail(
                    WorkoutInfo(
                        dto.id,
                        Instant.parse(dto.createdAt),
                        dto.start?.let(Instant::parse),
                        dto.end?.let(Instant::parse),
                        dto.duration?.decimal(),
                        dto.calories?.decimal(),
                        dto.imported,
                    ),
                    exercises.await().map { exercise ->
                        require(exercise.id > 0 && exercise.name.isNotBlank())
                        WorkoutExercise(
                            exercise.id,
                            exercise.name,
                            exercise.catalogId,
                            exercise.catalog?.domain(),
                            exercise.sets.orEmpty().map {
                                require(it.number > 0 && it.reps in 1..1000)
                                WorkoutSet(it.number, it.reps, it.weight?.decimal())
                            },
                        )
                    },
                )
            }
        }

    override suspend fun search(query: String): Outcome<List<CatalogExercise>> =
        request {
            api
                .search(query)
                .candidates
                .orEmpty()
                .map { it.domain() }
        }

    override suspend fun record(date: LocalDate?): Outcome<Long> =
        request(mutation = true) {
            api.record(date?.toString()).id.also {
                require(
                    it > 0,
                )
            }
        }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun saveExercise(
        visitId: Long,
        exerciseId: Long?,
        input: ExerciseInput,
    ): Outcome<Unit> =
        request(mutation = true) {
            api.saveExercise(
                visitId,
                exerciseId,
                ExerciseRequest(
                    input.name,
                    input.sets.map {
                        ExerciseSetRequest(
                            it.reps,
                            it.weight?.let { weight ->
                                JsonUnquotedLiteral(weight.toString())
                            },
                        )
                    },
                    input.catalogId,
                    input.rememberAlias,
                ),
            )
        }

    override suspend fun deleteWorkout(id: Long): Outcome<Unit> = request(mutation = true) { api.deleteWorkout(id) }

    override suspend fun deleteExercise(
        visitId: Long,
        exerciseId: Long,
    ): Outcome<Unit> = request(mutation = true) { api.deleteExercise(visitId, exerciseId) }

    private fun JsonPrimitive.decimal() =
        content.toBigDecimal().also {
            require(
                !isString && it.signum() >= 0,
            )
        }

    private fun CatalogExerciseDto.domain(): CatalogExercise {
        require(id.isNotBlank() && name.isNotBlank())
        return CatalogExercise(
            id,
            name,
            equipment,
            data?.primaryMuscles.orEmpty(),
            data?.instructions.orEmpty(),
            matchType,
        )
    }
}
