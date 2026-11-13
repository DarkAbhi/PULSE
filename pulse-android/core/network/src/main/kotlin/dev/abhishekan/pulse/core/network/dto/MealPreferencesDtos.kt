package dev.abhishekan.pulse.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MealPreferenceTimeDto(
    val kind: String,
    @SerialName("usual_time") val usualTime: String,
)

@Serializable
data class MealPreferencesDto(
    val goal: String,
    val barriers: List<String>,
    @SerialName("dietary_pattern") val dietaryPattern: String,
    @SerialName("food_avoidances") val foodAvoidances: List<String>,
    val meals: List<MealPreferenceTimeDto>,
    @SerialName("planning_slots_configured") val planningSlotsConfigured: Boolean = false,
)

@Serializable
data class MealOnboardingDto(
    val status: String,
    @SerialName("questionnaire_version") val questionnaireVersion: Int,
    val preferences: MealPreferencesDto? = null,
)

@Serializable
data class MealOnboardingInput(
    val status: String,
    @SerialName("questionnaire_version") val questionnaireVersion: Int,
    val preferences: MealPreferencesDto? = null,
)

@Serializable
data class MealOnboardingConflictDto(
    val state: MealOnboardingDto,
)
