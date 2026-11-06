package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.ProfileRepository

class ChangePassword(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(
        current: String,
        next: String,
        confirmation: String,
    ): Outcome<Unit> =
        if (AccountValidation.validPassword(
                current,
                next,
                confirmation,
            )
        ) {
            repository.changePassword(current, next)
        } else {
            Outcome.Error(Failure.InvalidInput)
        }
}
