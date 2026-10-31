package dev.abhishekan.pulse.core.data
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.MealChange
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.MealPlanRepository
import dev.abhishekan.pulse.core.network.MealPlanApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate

class PulseMealPlanRepository(
    private val api: MealPlanApi,
    private val auth: PulseAuthRepository,
) : MealPlanRepository {
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
            if (mutation) revision.update { it + 1 }
            Outcome.Error(failure)
        }
    }

    override suspend fun meals(
        start: LocalDate,
        end: LocalDate,
    ) = request {
        api.meals(start.toString(), end.toString()).map {
            it.domain().also { meal ->
                require(
                    meal.date in start..end,
                )
            }
        }
    }

    override suspend fun times() = request { api.times().map { it.domain() }.sortedBy { it.start } }

    override suspend fun apply(change: MealChange): Outcome<Unit> =
        request(true) {
            when (change) {
                is MealChange.Save -> {
                    val body =
                        buildJsonObject {
                            put("name", change.name.trim())
                            put("date", change.date.toString())
                            // Null cannot clear an existing time in this API; UI preserves that relationship.
                            change.timeId?.let { put("meal_time_id", it) }
                        }
                    val id = change.id
                    if (id == null) api.create("meal-plans", body) else api.update(id, body)
                }
                is MealChange.Delete -> api.delete("meal-plans/${change.id}")
                is MealChange.CreateTime ->
                    api.create(
                        "meal-times",
                        buildJsonObject {
                            put("name", change.name.trim())
                            put("start_time", change.start.toString())
                            put("end_time", change.end.toString())
                        },
                    )
                is MealChange.DeleteTime -> api.delete("meal-times/${change.time.id}")
            }
        }

    override suspend fun consumed(
        id: Long,
        value: Boolean,
    ) = request(true) {
        api.consumed(id, buildJsonObject { put("is_consumed", value) }).domain().also {
            require(
                it.id == id && it.consumed == value,
            )
        }
    }
}
