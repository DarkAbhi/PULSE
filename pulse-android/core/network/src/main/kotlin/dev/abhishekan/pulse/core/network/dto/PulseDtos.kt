package dev.abhishekan.pulse.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable class SessionDto(
    val username: String,
)

@Serializable class ProfileDto(
    @SerialName(
        "has_profile",
    ) val hasProfile: Boolean,
    val name: String? = null,
)

@Serializable class WorkoutDto(
    val visited: Boolean,
)

@Serializable class HorizonDto(
    val currency: String,
    @SerialName("base_amount") val baseAmount: JsonPrimitive,
    @SerialName("remaining_amount") val remainingAmount: JsonPrimitive,
    @SerialName("total_deductions") val deductions: JsonPrimitive,
)

@Serializable class NotificationDto(
    val id: Long,
    val title: String,
    val body: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
