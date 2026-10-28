package dev.abhishekan.pulse.core.data

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Horizon
import dev.abhishekan.pulse.core.domain.model.Notification
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.domain.model.WorkoutToday
import dev.abhishekan.pulse.core.domain.repository.DashboardRepository
import dev.abhishekan.pulse.core.network.PulseApi
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal
import java.time.Instant

class PulseDashboardRepository(
    private val api: PulseApi,
    private val auth: PulseAuthRepository,
) : DashboardRepository {
    private val revision = kotlinx.coroutines.flow.MutableStateFlow(0L)
    override val changes: kotlinx.coroutines.flow.Flow<Long> = revision.asStateFlow()

    internal fun accountChanged() {
        revision.update { it + 1 }
    }

    private suspend fun <T> fetch(block: suspend () -> T): Outcome<T> {
        val version = auth.version
        return try {
            Outcome.Success(block())
        } catch (error: Exception) {
            val failure = remoteFailure(error)
            if (failure == Failure.Unauthorized) auth.expire(version)
            Outcome.Error(failure)
        }
    }

    override suspend fun profile(): Outcome<Profile> =
        fetch {
            val dto = api.profile()
            Profile(if (dto.hasProfile) dto.name?.trim()?.takeIf { it.isNotEmpty() } else null, dto.hasProfile, dto.timezone)
        }

    override suspend fun workout(): Outcome<WorkoutToday> =
        fetch {
            WorkoutToday(api.workout().visited)
        }

    override suspend fun horizon(): Outcome<Horizon> =
        fetch {
            val dto = api.horizon()
            require(dto.currency.isNotBlank())
            Horizon(
                dto.currency,
                dto.baseAmount.decimal(),
                dto.remainingAmount.decimal(),
                dto.deductions.decimal(),
            )
        }

    override suspend fun notifications(): Outcome<List<Notification>> =
        fetch {
            api.notifications().map {
                require(it.id > 0 && it.title.isNotBlank())
                Notification(
                    it.id,
                    it.title,
                    it.body?.takeIf(String::isNotBlank),
                    it.createdAt?.let(Instant::parse),
                    it.source,
                    it.priority,
                    it.targetPath,
                )
            }
        }

    private fun JsonPrimitive.decimal(): BigDecimal {
        require(!isString)
        return content.toBigDecimal()
    }
}
