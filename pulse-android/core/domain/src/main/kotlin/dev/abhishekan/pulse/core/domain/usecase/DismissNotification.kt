package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.NotificationsRepository

class DismissNotification(
    private val repository: NotificationsRepository,
) {
    suspend operator fun invoke(id: Long): Outcome<Unit> =
        if (id >
            0
        ) {
            repository.dismiss(id)
        } else {
            Outcome.Error(Failure.InvalidInput)
        }
}
