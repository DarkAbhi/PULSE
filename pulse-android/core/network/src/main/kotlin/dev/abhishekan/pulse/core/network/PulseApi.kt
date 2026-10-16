package dev.abhishekan.pulse.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable class LoginRequest(val username: String, val password: String)
@Serializable class SessionDto(val username: String)
@Serializable class ProfileDto(@SerialName("has_profile") val hasProfile: Boolean, val name: String? = null)
@Serializable class WorkoutDto(val visited: Boolean)
@Serializable class HorizonDto(
    val currency: String,
    @SerialName("base_amount") val baseAmount: JsonPrimitive,
    @SerialName("remaining_amount") val remainingAmount: JsonPrimitive,
    @SerialName("total_deductions") val deductions: JsonPrimitive,
)
@Serializable class NotificationDto(val id: Long, val title: String, val body: String? = null,
    @SerialName("created_at") val createdAt: String? = null)

class PulseApi(private val client: HttpClient) {
    suspend fun login(username: String, password: String): SessionDto = client.post("auth/login") { setBody(LoginRequest(username, password)) }.body()
    suspend fun session(): SessionDto = client.get("auth/session").body()
    suspend fun logout() { client.post("auth/logout") }
    suspend fun profile(): ProfileDto = client.get("profile").body()
    suspend fun workout(): WorkoutDto = client.get("workout/today").body()
    suspend fun horizon(): HorizonDto = client.get("horizon").body()
    suspend fun notifications(): List<NotificationDto> = client.get("notifications?limit=5").body()
}
