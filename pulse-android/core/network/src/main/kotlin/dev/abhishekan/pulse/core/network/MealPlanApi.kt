package dev.abhishekan.pulse.core.network
import dev.abhishekan.pulse.core.network.dto.MealOnboardingDto
import dev.abhishekan.pulse.core.network.dto.MealOnboardingInput
import dev.abhishekan.pulse.core.network.dto.MealPreferencesDto
import dev.abhishekan.pulse.core.network.dto.MealTimeDto
import dev.abhishekan.pulse.core.network.dto.PlannedMealDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.serialization.json.JsonObject

class MealPlanApi(
    private val client: HttpClient,
) {
    suspend fun onboarding(): MealOnboardingDto = client.get("meal-plan-onboarding") { parameter("planning_slots", 1) }.body()

    suspend fun resolveOnboarding(input: MealOnboardingInput): MealOnboardingDto =
        client.put("meal-plan-onboarding") {
            parameter("planning_slots", 1)
            setBody(input)
        }.body()

    suspend fun savePreferences(input: MealPreferencesDto): MealOnboardingDto =
        client.put("meal-preferences") {
            parameter("planning_slots", 1)
            setBody(input)
        }.body()

    suspend fun meals(
        start: String,
        end: String,
    ): List<PlannedMealDto> =
        client
            .get("meal-plans") {
                parameter("start_date", start)
                parameter("end_date", end)
            }.body()

    suspend fun times(): List<MealTimeDto> = client.get("meal-times").body()

    suspend fun create(
        path: String,
        body: JsonObject,
    ) {
        client.post(path) { setBody(body) }
    }

    suspend fun update(
        id: Long,
        body: JsonObject,
    ) {
        client.patch("meal-plans/$id") { setBody(body) }
    }

    suspend fun delete(path: String) {
        client.delete(path)
    }

    suspend fun consumed(
        id: Long,
        body: JsonObject,
    ): PlannedMealDto = client.patch("meal-plans/$id/consumed") { setBody(body) }.body()
}
