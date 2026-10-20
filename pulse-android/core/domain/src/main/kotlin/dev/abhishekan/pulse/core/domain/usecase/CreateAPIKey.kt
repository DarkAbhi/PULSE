package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.model.CreatedAPIKey
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.ProfileRepository

class CreateAPIKey(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(name: String): Outcome<CreatedAPIKey> =
        if (AccountValidation.validName(
                name,
            )
        ) {
            repository.createKey(AccountValidation.trimmedName(name))
        } else {
            Outcome.Error(Failure.InvalidInput)
        }
}
