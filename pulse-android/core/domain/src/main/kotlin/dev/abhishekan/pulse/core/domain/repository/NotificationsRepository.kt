package dev.abhishekan.pulse.core.domain.repository
import dev.abhishekan.pulse.core.domain.model.Notification
import dev.abhishekan.pulse.core.domain.model.Outcome

interface NotificationsRepository {
    suspend fun list(): Outcome<List<Notification>>

    suspend fun dismiss(id: Long): Outcome<Unit>

    suspend fun clear(): Outcome<Unit>
}
