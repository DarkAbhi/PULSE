package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.repository.ProfileRepository

class SaveProfileCurrency(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(currency: String): Outcome<String> =
        if (currency in UserCurrency.available) {
            repository.saveCurrency(currency)
        } else {
            Outcome.Error(Failure.InvalidInput)
        }
}
