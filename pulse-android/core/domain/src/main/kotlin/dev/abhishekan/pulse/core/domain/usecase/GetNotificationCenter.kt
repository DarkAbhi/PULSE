package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.repository.NotificationsRepository

class GetNotificationCenter(
    private val repository: NotificationsRepository,
) {
    suspend operator fun invoke() = repository.list()
}
