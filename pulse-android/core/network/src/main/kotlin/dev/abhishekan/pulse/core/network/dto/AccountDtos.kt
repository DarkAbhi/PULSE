package dev.abhishekan.pulse.core.network.dto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable class TimeZoneDto(
    val timezone: String,
)

@Serializable class NameRequest(
    val name: String,
)

@Serializable class SavedNameDto(
    val name: String,
)

@Serializable class PasswordRequest(
    @SerialName("current_password") val current: String,
    @SerialName("new_password") val next: String,
)

@Serializable class APIKeyDto(
    val id: Long,
    val name: String,
    @SerialName("token_prefix") val prefix: String,
    @SerialName("created_at") val createdAt: String,
    val token: String? = null,
)
