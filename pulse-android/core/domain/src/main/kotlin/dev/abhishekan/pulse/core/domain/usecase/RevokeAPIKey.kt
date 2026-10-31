package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.ProfileRepository

class RevokeAPIKey(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(id: Long): Outcome<Unit> =
        if (id >
            0
        ) {
            repository.revokeKey(id)
        } else {
            Outcome.Error(Failure.InvalidInput)
        }
}
