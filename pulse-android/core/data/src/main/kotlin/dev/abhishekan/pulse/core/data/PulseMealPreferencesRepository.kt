package dev.abhishekan.pulse.core.data

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.MealOnboarding
import dev.abhishekan.pulse.core.domain.model.MealOnboardingStatus
import dev.abhishekan.pulse.core.domain.model.MealPreferenceTime
import dev.abhishekan.pulse.core.domain.model.MealPreferences
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.MealPreferencesRepository
import dev.abhishekan.pulse.core.network.MealPlanApi
import dev.abhishekan.pulse.core.network.dto.MealOnboardingConflictDto
import dev.abhishekan.pulse.core.network.dto.MealOnboardingDto
import dev.abhishekan.pulse.core.network.dto.MealOnboardingInput
import dev.abhishekan.pulse.core.network.dto.MealPreferenceTimeDto
import dev.abhishekan.pulse.core.network.dto.MealPreferencesDto
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class PulseMealPreferencesRepository(
    private val api: MealPlanApi,
    private val auth: PulseAuthRepository,
) : MealPreferencesRepository {
    private suspend fun request(block: suspend () -> MealOnboardingDto): Outcome<MealOnboarding> {
        val version = auth.version
        return try {
            val result = block().domain()
            // A response from a previous account must never enter the next account's UI.
            if (version != auth.version) Outcome.Error(Failure.Unauthorized) else Outcome.Success(result)
        } catch (error: Exception) {
            val failure = remoteFailure(error)
            if (failure == Failure.Unauthorized) auth.expire(version)
            Outcome.Error(failure)
        }
    }

    override suspend fun onboarding() = request { api.onboarding() }

    override suspend fun resolve(preferences: MealPreferences?): Outcome<MealOnboarding> {
        if (preferences != null && preferences.invalidStep() != null) return Outcome.Error(Failure.InvalidInput)
        return request {
            try {
                api.resolveOnboarding(
                    MealOnboardingInput(
                        if (preferences ==
                            null
                        ) {
                            "skipped"
                        } else {
                            "completed"
                        },
                        questionnaireVersion = 1,
                        preferences = preferences?.dto(),
                    ),
                )
            } catch (error: ResponseException) {
                if (error.response.status.value != 409) throw error
                error.response
                    .body<MealOnboardingConflictDto>()
                    .state
                    .also { require(it.status != "pending") }
            }
        }
    }

    override suspend fun save(preferences: MealPreferences): Outcome<MealOnboarding> {
        if (preferences.invalidStep() != null) return Outcome.Error(Failure.InvalidInput)
        return request { api.savePreferences(preferences.dto()) }
    }
}

private val preferenceTimeFormat = DateTimeFormatter.ofPattern("HH:mm")

private fun MealPreferences.dto() =
    MealPreferencesDto(
        goal,
        barriers,
        dietaryPattern,
        foodAvoidances,
        meals.map {
            MealPreferenceTimeDto(it.kind, it.time.format(preferenceTimeFormat))
        },
    )

private fun MealOnboardingDto.domain(): MealOnboarding {
    require(questionnaireVersion == 1)
    val status =
        when (status) {
            "pending" -> MealOnboardingStatus.Pending
            "skipped" -> MealOnboardingStatus.Skipped
            "completed" -> MealOnboardingStatus.Completed
            else -> error("Unknown meal setup status")
        }
    val p =
        preferences?.let { dto ->
            MealPreferences(
                dto.goal,
                dto.barriers,
                dto.dietaryPattern,
                dto.foodAvoidances,
                dto.meals.map {
                    require(Regex("([01]\\d|2[0-3]):[0-5]\\d").matches(it.usualTime))
                    MealPreferenceTime(it.kind, LocalTime.parse(it.usualTime))
                },
            ).also { require(it.invalidStep() == null) }
        }
    require(status != MealOnboardingStatus.Completed || p != null)
    require(status != MealOnboardingStatus.Pending || p == null)
    return MealOnboarding(status, p)
}
