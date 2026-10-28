package dev.abhishekan.pulse.core.domain.repository
import dev.abhishekan.pulse.core.domain.model.APIKey
import dev.abhishekan.pulse.core.domain.model.CreatedAPIKey
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.Profile

interface ProfileRepository {
    suspend fun profile(): Outcome<Profile>

    suspend fun saveTimeZone(timezone: String): Outcome<String>

    suspend fun saveName(name: String): Outcome<Profile>

    suspend fun changePassword(
        current: String,
        next: String,
    ): Outcome<Unit>

    suspend fun keys(): Outcome<List<APIKey>>

    suspend fun createKey(name: String): Outcome<CreatedAPIKey>

    suspend fun revokeKey(id: Long): Outcome<Unit>
}
