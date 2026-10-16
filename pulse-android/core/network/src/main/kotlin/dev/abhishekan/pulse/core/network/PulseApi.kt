package dev.abhishekan.pulse.core.network

import dev.abhishekan.pulse.core.network.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class PulseApi(private val client: HttpClient) {
    suspend fun login(username: String, password: String): SessionDto = client.post("auth/login") { setBody(LoginRequest(username, password)) }.body()
    suspend fun session(): SessionDto = client.get("auth/session").body()
    suspend fun logout() { client.post("auth/logout") }
    suspend fun profile(): ProfileDto = client.get("profile").body()
    suspend fun workout(): WorkoutDto = client.get("workout/today").body()
    suspend fun horizon(): HorizonDto = client.get("horizon").body()
    suspend fun notifications(): List<NotificationDto> = client.get("notifications?limit=5").body()
}
