package dev.abhishekan.pulse.core.data
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Notification
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.NotificationsRepository
import dev.abhishekan.pulse.core.network.PulseApi
import java.time.Instant

class PulseNotificationsRepository(
    private val api: PulseApi,
    private val auth: PulseAuthRepository,
    private val dashboard: PulseDashboardRepository,
) : NotificationsRepository {
    private suspend fun <T> request(
        mutation: Boolean = false,
        block: suspend () -> T,
    ): Outcome<T> =
        auth.authenticatedRequest(onResult = { if (mutation || it is Outcome.Success) dashboard.accountChanged() }) {
            block()
        }

    override suspend fun list() =
        request {
            api.notificationCenter().map {
                require(it.id > 0 && it.title.isNotBlank())
                Notification(it.id, it.title, it.body, it.createdAt?.let(Instant::parse), it.source, it.priority, it.targetPath)
            }
        }

    override suspend fun dismiss(id: Long): Outcome<Unit> {
        val result = request(true) { api.dismissNotification(id) }
        return if (result ==
            Outcome.Error(Failure.NotFound)
        ) {
            Outcome.Success(Unit)
        } else {
            result
        }
    }

    override suspend fun clear() = request(true) { api.clearNotifications() }
}
