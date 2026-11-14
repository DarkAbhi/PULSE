package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.domain.repository.ProfileRepository

class SaveProfileTimeZone(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(timezone: String): Outcome<String> =
        if (timezone in UserTimeZone.available) {
            repository.saveTimeZone(timezone)
        } else {
            Outcome.Error(Failure.InvalidInput)
        }
}
