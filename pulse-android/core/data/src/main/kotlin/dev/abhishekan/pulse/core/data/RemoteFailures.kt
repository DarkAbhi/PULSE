package dev.abhishekan.pulse.core.data

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.network.SessionStorageException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.JsonConvertException
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.DateTimeException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

internal fun remoteFailure(error: Exception): Failure {
    if (error is CancellationException) throw error
    if (generateSequence<Throwable>(error) { it.cause }.any { it is SessionStorageException }) return Failure.Storage
    return when (error) {
        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException -> Failure.Timeout
        is ResponseException -> if (error.response.status.value == 401) Failure.Unauthorized else Failure.Server
        is JsonConvertException, is SerializationException, is IllegalArgumentException, is DateTimeException -> Failure.Malformed
        is IOException -> Failure.Connection
        else -> Failure.Server
    }
}
