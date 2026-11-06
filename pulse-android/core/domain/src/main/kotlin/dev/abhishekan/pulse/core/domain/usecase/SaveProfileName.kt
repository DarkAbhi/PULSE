package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.domain.repository.ProfileRepository

class SaveProfileName(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(name: String): Outcome<Profile> =
        if (AccountValidation.validName(
                name,
            )
        ) {
            repository.saveName(AccountValidation.trimmedName(name))
        } else {
            Outcome.Error(Failure.InvalidInput)
        }
}
