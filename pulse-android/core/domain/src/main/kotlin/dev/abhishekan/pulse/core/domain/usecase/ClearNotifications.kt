package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.repository.NotificationsRepository

class ClearNotifications(
    private val repository: NotificationsRepository,
) {
    suspend operator fun invoke() = repository.clear()
}
