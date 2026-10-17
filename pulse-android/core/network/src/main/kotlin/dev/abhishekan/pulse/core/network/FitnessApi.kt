package dev.abhishekan.pulse.core.network
import dev.abhishekan.pulse.core.network.dto.CatalogSearchDto
import dev.abhishekan.pulse.core.network.dto.ExerciseDto
import dev.abhishekan.pulse.core.network.dto.ExerciseRequest
import dev.abhishekan.pulse.core.network.dto.FitnessOverviewDto
import dev.abhishekan.pulse.core.network.dto.RecordVisitRequest
import dev.abhishekan.pulse.core.network.dto.RecordVisitResponse
import dev.abhishekan.pulse.core.network.dto.VisitDto
import dev.abhishekan.pulse.core.network.dto.WorkoutInfoDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.query
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url

class FitnessApi(
    private val client: HttpClient,
) {
    suspend fun overview(): FitnessOverviewDto = client.get("fitness/overview").body()

    suspend fun visits(): List<VisitDto> = client.get("gym-visits").body()

    suspend fun workout(id: Long): WorkoutInfoDto = client.get("fitness-workouts/$id").body()

    suspend fun exercises(id: Long): List<ExerciseDto> = client.get("gym-visits/$id/exercises").body()

    suspend fun search(query: String): CatalogSearchDto =
        client
            .get("exercise-catalog") {
                url { parameters.append("q", query) }
            }.body()

    suspend fun record(date: String?): RecordVisitResponse =
        if (date ==
            null
        ) {
            client.post("workout/today").body()
        } else {
            client.post("gym-visits") { setBody(RecordVisitRequest(date)) }.body()
        }

    suspend fun saveExercise(
        visitId: Long,
        exerciseId: Long?,
        request: ExerciseRequest,
    ) {
        if (exerciseId == null) {
            client.post("gym-visits/$visitId/exercises") { setBody(request) }
        } else {
            client.put("gym-visits/$visitId/exercises/$exerciseId") { setBody(request) }
        }
    }

    suspend fun deleteWorkout(id: Long) {
        client.delete("gym-visits/$id")
    }

    suspend fun deleteExercise(
        visitId: Long,
        exerciseId: Long,
    ) {
        client.delete("gym-visits/$visitId/exercises/$exerciseId")
    }
}
