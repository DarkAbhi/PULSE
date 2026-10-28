package dev.abhishekan.pulse.core.data

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.SessionState
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.domain.repository.AuthRepository
import dev.abhishekan.pulse.core.network.PersistentSessionCookies
import dev.abhishekan.pulse.core.network.PulseApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Clock

class PulseAuthRepository(
    private val api: PulseApi,
    private val cookies: PersistentSessionCookies,
    private val clock: Clock,
) : AuthRepository {
    private val mutableSession = MutableStateFlow<SessionState>(SessionState.Checking)
    override val session = mutableSession.asStateFlow()
    private val mutex = Mutex()

    @Volatile internal var version = 0L
        private set

    override suspend fun restore() =
        mutex.withLock {
            mutableSession.value = SessionState.Checking
            try {
                val expires = cookies.expiresAt()
                if (expires == null) {
                    UserTimeZone.reset()
                    mutableSession.value = SessionState.SignedOut()
                } else {
                    val user = api.session()
                    require(user.username.isNotBlank())
                    if (expires <= clock.millis()) {
                        endSession(expired = true)
                    } else {
                        UserTimeZone.select(api.profile().timezone)
                        version++
                        mutableSession.value = SessionState.Authenticated(user.username, expires)
                    }
                }
            } catch (error: Exception) {
                val failure = remoteFailure(error)
                if (failure == Failure.Unauthorized) {
                    endSession(expired = true)
                } else {
                    mutableSession.value = SessionState.Unavailable(failure)
                }
            }
            Unit
        }

    override suspend fun login(
        username: String,
        password: String,
    ): Outcome<Unit> =
        mutex.withLock {
            if (mutableSession.value !is SessionState.SignedOut) {
                return@withLock Outcome.Error(
                    Failure.InvalidInput,
                )
            }
            try {
                cookies.clear()
                val user = api.login(username, password)
                require(user.username.isNotBlank())
                val expires = requireNotNull(cookies.expiresAt())
                UserTimeZone.select(api.profile().timezone)
                version++
                mutableSession.value = SessionState.Authenticated(user.username, expires)
                Outcome.Success(Unit)
            } catch (error: Exception) {
                val failure = remoteFailure(error)
                Outcome.Error(
                    if (failure ==
                        Failure.Unauthorized
                    ) {
                        Failure.InvalidCredentials
                    } else {
                        failure
                    },
                )
            }
        }

    override suspend fun logout(): Outcome<Unit> =
        mutex.withLock {
            try {
                api.logout()
                endSession(expired = false)
            } catch (error: Exception) {
                val failure = remoteFailure(error)
                if (failure == Failure.Unauthorized ||
                    failure == Failure.Storage
                ) {
                    endSession(expired = true)
                } else {
                    Outcome.Error(failure)
                }
            }
        }

    override suspend fun forget(): Outcome<Unit> =
        mutex.withLock {
            try {
                cookies.clear()
                version++
                UserTimeZone.reset()
                mutableSession.value = SessionState.SignedOut()
                Outcome.Success(Unit)
            } catch (error: Exception) {
                val failure = remoteFailure(error)
                mutableSession.value = SessionState.Unavailable(failure)
                Outcome.Error(failure)
            }
        }

    internal suspend fun expire(expectedVersion: Long) =
        mutex.withLock {
            if (version == expectedVersion &&
                mutableSession.value is SessionState.Authenticated
            ) {
                endSession(expired = true)
            }
        }

    override suspend fun checkExpiry() =
        mutex.withLock {
            val current = mutableSession.value
            if (current is SessionState.Authenticated &&
                current.expiresAtMillis <= clock.millis()
            ) {
                endSession(expired = true)
            }
            Unit
        }

    private suspend fun endSession(expired: Boolean): Outcome<Unit> {
        version++
        UserTimeZone.reset()
        mutableSession.value = SessionState.SignedOut(expired)
        // Finish local revocation even when removing authenticated navigation cancels the caller.
        return withContext(NonCancellable) {
            try {
                cookies.clear()
                Outcome.Success(Unit)
            } catch (error: Exception) {
                val failure = remoteFailure(error)
                UserTimeZone.reset()
                mutableSession.value = SessionState.SignedOut(expired, storageProblem = true)
                Outcome.Error(failure)
            }
        }
    }
}
