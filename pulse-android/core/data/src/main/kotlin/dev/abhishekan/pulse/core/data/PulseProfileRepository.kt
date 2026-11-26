package dev.abhishekan.pulse.core.data

import dev.abhishekan.pulse.core.domain.model.APIKey
import dev.abhishekan.pulse.core.domain.model.CreatedAPIKey
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.domain.repository.ProfileRepository
import dev.abhishekan.pulse.core.domain.usecase.AccountValidation
import dev.abhishekan.pulse.core.network.PulseApi
import dev.abhishekan.pulse.core.network.dto.APIKeyDto
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.ZoneId

class PulseProfileRepository(
    private val api: PulseApi,
    private val auth: PulseAuthRepository,
    private val dashboard: PulseDashboardRepository,
) : ProfileRepository {
    private suspend fun <T> request(
        mutation: Boolean = false,
        refresh: Boolean = false,
        password: Boolean = false,
        block: suspend (Long) -> T,
    ): Outcome<T> =
        auth.authenticatedRequest(
            onResult = { if (mutation || (refresh && it is Outcome.Success)) dashboard.accountChanged() },
            mapFailure = { if (password) passwordFailure(it) else remoteFailure(it) },
            block = block,
        )

    private suspend fun passwordFailure(error: Exception): Failure {
        if (error is ResponseException && error.response.status.value == 400) {
            // Only this known error is interpreted; arbitrary server text is never shown.
            val wrongPassword =
                try {
                    error.response.bodyAsText().contains("current password is incorrect")
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    false
                }
            if (wrongPassword) return Failure.InvalidCredentials
        }
        return remoteFailure(error)
    }

    override suspend fun profile() =
        request(refresh = true) { version ->
            val previousZone = UserTimeZone.zone
            val previousCurrency = UserCurrency.code
            val dto = api.profile()
            ZoneId.of(dto.timezone)
            require(UserCurrency.valid(dto.currency))
            auth.withCurrentVersion(version) {
                if (previousCurrency == UserCurrency.code) UserCurrency.select(dto.currency)
                if (previousZone == UserTimeZone.zone) UserTimeZone.select(dto.timezone)
            }
            Profile(dto.name?.takeIf { dto.hasProfile && it.isNotBlank() }, dto.hasProfile, dto.timezone, dto.currency)
        }

    override suspend fun saveName(name: String) =
        request(true) {
            api.saveProfile(name).let {
                require(
                    AccountValidation
                        .validName(it.name),
                )
                Profile(it.name, true, UserTimeZone.zone.id, UserCurrency.code)
            }
        }

    override suspend fun saveCurrency(currency: String) =
        request(mutation = true) { version ->
            api.saveCurrency(currency).currency.also {
                require(it == currency && UserCurrency.valid(it))
                auth.withCurrentVersion(version) { UserCurrency.select(it) }
            }
        }

    override suspend fun saveTimeZone(timezone: String) =
        request(mutation = true) { version ->
            api.saveTimeZone(timezone).timezone.also {
                require(it == timezone)
                auth.withCurrentVersion(version) { UserTimeZone.select(it) }
            }
        }

    override suspend fun changePassword(
        current: String,
        next: String,
    ) = request(password = true) { api.password(current, next) }

    private fun APIKeyDto.domain(): APIKey {
        require(id > 0 && name.isNotBlank() && prefix.isNotBlank())
        return APIKey(id, name, prefix, Instant.parse(createdAt))
    }

    override suspend fun keys() = request { api.keys().orEmpty().map { it.domain() } }

    override suspend fun createKey(name: String) =
        request(true) {
            val dto = api.createKey(name)
            val token = requireNotNull(dto.token)
            require(token.matches(Regex("lt_[a-f0-9]{64}")))
            CreatedAPIKey(dto.domain(), token)
        }

    override suspend fun revokeKey(id: Long): Outcome<Unit> {
        val result = request(true) { api.revokeKey(id) }
        return if (result ==
            Outcome.Error(Failure.NotFound)
        ) {
            Outcome.Success(Unit)
        } else {
            result
        }
    }
}
