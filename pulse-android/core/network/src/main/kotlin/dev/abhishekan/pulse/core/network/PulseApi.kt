package dev.abhishekan.pulse.core.network
import dev.abhishekan.pulse.core.network.dto.APIKeyDto
import dev.abhishekan.pulse.core.network.dto.HorizonDto
import dev.abhishekan.pulse.core.network.dto.LoginRequest
import dev.abhishekan.pulse.core.network.dto.NameRequest
import dev.abhishekan.pulse.core.network.dto.NotificationDto
import dev.abhishekan.pulse.core.network.dto.PasswordRequest
import dev.abhishekan.pulse.core.network.dto.ProfileDto
import dev.abhishekan.pulse.core.network.dto.SavedNameDto
import dev.abhishekan.pulse.core.network.dto.SessionDto
import dev.abhishekan.pulse.core.network.dto.WorkoutDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

class PulseApi(
    private val client: HttpClient,
) {
    suspend fun login(
        username: String,
        password: String,
    ): SessionDto = client.post("auth/login") { setBody(LoginRequest(username, password)) }.body()

    suspend fun session(): SessionDto = client.get("auth/session").body()

    suspend fun logout() {
        client.post("auth/logout")
    }

    suspend fun profile(): ProfileDto = client.get("profile").body()

    suspend fun workout(): WorkoutDto = client.get("workout/today").body()

    suspend fun horizon(): HorizonDto = client.get("horizon").body()

    suspend fun saveProfile(name: String): SavedNameDto = client.put("profile") { setBody(NameRequest(name)) }.body()

    suspend fun password(
        current: String,
        next: String,
    ) {
        client.put("profile/password") { setBody(PasswordRequest(current, next)) }
    }

    suspend fun keys(): List<APIKeyDto>? = client.get("profile/api-keys").body()

    suspend fun createKey(name: String): APIKeyDto = client.post("profile/api-keys") { setBody(NameRequest(name)) }.body()

    suspend fun revokeKey(id: Long) {
        client.delete("profile/api-keys/$id")
    }

    suspend fun notificationCenter(): List<NotificationDto> = client.get("notifications").body()

    suspend fun dismissNotification(id: Long) {
        client.delete("notifications/$id")
    }

    suspend fun clearNotifications() {
        client.delete("notifications")
    }

    suspend fun notifications(): List<NotificationDto> = client.get("notifications?limit=5").body()
}
