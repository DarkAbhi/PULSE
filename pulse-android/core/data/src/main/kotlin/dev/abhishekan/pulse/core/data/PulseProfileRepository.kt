package dev.abhishekan.pulse.core.data

import dev.abhishekan.pulse.core.domain.model.APIKey
import dev.abhishekan.pulse.core.domain.model.CreatedAPIKey
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.domain.repository.ProfileRepository
import dev.abhishekan.pulse.core.network.PulseApi
import dev.abhishekan.pulse.core.network.dto.APIKeyDto
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import java.time.Instant

class PulseProfileRepository(
    private val api: PulseApi,
    private val auth: PulseAuthRepository,
    private val dashboard: PulseDashboardRepository,
) : ProfileRepository {
    private suspend fun <T> request(
        mutation: Boolean = false,
        password: Boolean = false,
        block: suspend () -> T,
    ): Outcome<T> {
        val version = auth.version
        return try {
            val value = block()
            if (mutation) dashboard.accountChanged()
            Outcome.Success(value)
        } catch (e: Exception) {
            var failure = remoteFailure(e)
            // Only this known error is interpreted; arbitrary server text is never shown.
            if (password &&
                e is ResponseException &&
                e.response.status.value == 400 &&
                try {
                    e.response.bodyAsText().contains("current password is incorrect")
                } catch (
                    read: Exception,
                ) {
                    remoteFailure(read)
                    false
                }
            ) {
                failure =
                    Failure.InvalidCredentials
            }
            if (failure == Failure.Unauthorized) auth.expire(version)
            if (mutation) dashboard.accountChanged()
            Outcome.Error(failure)
        }
    }

    override suspend fun profile() =
        request {
            val version = auth.version
            val previousZone = UserTimeZone.zone
            val previousCurrency = UserCurrency.code
            val dto = api.profile()
            java.time.ZoneId.of(dto.timezone)
            require(UserCurrency.valid(dto.currency))
            if (version == auth.version && previousCurrency == UserCurrency.code) UserCurrency.select(dto.currency)
            if (version == auth.version && previousZone == UserTimeZone.zone) UserTimeZone.select(dto.timezone)
            Profile(dto.name?.takeIf { dto.hasProfile && it.isNotBlank() }, dto.hasProfile, dto.timezone, dto.currency)
        }.also { if (it is Outcome.Success) dashboard.accountChanged() }

    override suspend fun saveName(name: String) =
        request(true) {
            api.saveProfile(name).let {
                require(
                    dev.abhishekan.pulse.core.domain.usecase.AccountValidation
                        .validName(it.name),
                )
                Profile(it.name, true, UserTimeZone.zone.id, UserCurrency.code)
            }
        }

    override suspend fun saveCurrency(currency: String) =
        request(true) {
            val version = auth.version
            api.saveCurrency(currency).currency.also {
                require(it == currency && UserCurrency.valid(it))
                if (version == auth.version) UserCurrency.select(it)
            }
        }

    override suspend fun saveTimeZone(timezone: String) =
        request(true) {
            val version = auth.version
            api.saveTimeZone(timezone).timezone.also {
                require(it == timezone)
                if (version == auth.version) UserTimeZone.select(it)
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
