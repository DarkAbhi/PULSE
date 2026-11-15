package dev.abhishekan.pulse.core.data

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal suspend fun <T> PulseAuthRepository.authenticatedRequest(
    onResult: (Outcome<T>) -> Unit = {},
    mapFailure: suspend (Exception) -> Failure = { remoteFailure(it) },
    block: suspend (Long) -> T,
): Outcome<T> {
    val startedAtVersion = version
    val result =
        try {
            Outcome.Success(block(startedAtVersion))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Outcome.Error(mapFailure(error))
        }

    currentCoroutineContext().ensureActive()
    // A failed mutation can still have committed. Notify observers only for the session that sent it.
    if (!withCurrentVersion(startedAtVersion) { onResult(result) }) return Outcome.Error(Failure.Unauthorized)
    if (result == Outcome.Error(Failure.Unauthorized)) expire(startedAtVersion)
    return result
}
