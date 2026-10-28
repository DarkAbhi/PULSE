package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.NotificationsRepository

class GetNotificationCount(
    private val repository: NotificationsRepository,
) {
    suspend operator fun invoke(): Outcome<Int> =
        when (val result = repository.list()) {
            is Outcome.Success -> Outcome.Success(result.value.size)
            is Outcome.Error -> result
        }
}
