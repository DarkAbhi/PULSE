package dev.abhishekan.pulse.core.network
import dev.abhishekan.pulse.core.network.dto.GarageHistoryDto
import dev.abhishekan.pulse.core.network.dto.VehicleCatalogOptionDto
import dev.abhishekan.pulse.core.network.dto.VehicleDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.onUpload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentDisposition
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readAvailable
import kotlinx.serialization.json.JsonObject
import java.io.ByteArrayOutputStream

class ReceiptTransferException : Exception("Receipt transfer unavailable")

class GarageApi(
    private val client: HttpClient,
) {
    suspend fun vehicles(): List<VehicleDto> = client.get("vehicles").body()

    suspend fun catalog(
        kind: String,
        makeSlug: String?,
        query: String,
    ): List<VehicleCatalogOptionDto> =
        client
            .get("vehicle-catalog/${if (makeSlug == null) "makes" else "models"}") {
                parameter("kind", kind)
                parameter("q", query)
                makeSlug?.let { parameter("make_slug", it) }
            }.body()

    suspend fun history(id: Long): GarageHistoryDto = client.get("vehicles/$id/history").body()

    suspend fun save(
        path: String,
        update: Boolean,
        body: JsonObject,
    ) {
        if (update) client.put(path) { setBody(body) } else client.post(path) { setBody(body) }
    }

    suspend fun delete(path: String) {
        client.delete(path)
    }

    suspend fun upload(
        vehicle: Long,
        record: Long,
        name: String,
        type: String,
        bytes: ByteArray,
        progress: (Float) -> Unit,
    ) {
        require(bytes.size in 1..LIMIT)
        client.post("vehicles/$vehicle/maintenance-records/$record/attachments") {
            val multipart =
                MultiPartFormDataContent(
                    formData {
                        append(
                            "file",
                            bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, type)
                                // Ktor supplies form-data/name; only the filename parameter belongs here.
                                append(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.File
                                        .withParameter(
                                            ContentDisposition.Parameters.FileName,
                                            name,
                                        ).toString()
                                        .substringAfter("; "),
                                )
                            },
                        )
                    },
                )
            contentType(multipart.contentType)
            setBody(multipart)
            timeout { requestTimeoutMillis = 120_000 }
            onUpload { sent, total ->
                if (total != null &&
                    total > 0
                ) {
                    progress((sent.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        }
    }

    suspend fun download(
        vehicle: Long,
        record: Long,
        receipt: Long,
    ): ByteArray {
        return client
            .prepareGet(
                "vehicles/$vehicle/maintenance-records/$record/attachments/$receipt",
            ) {
                expectSuccess =
                    false
            }.execute { response ->
                if (response.status.value !in 300..399) {
                    if (response.status.value ==
                        401
                    ) {
                        throw ClientRequestException(response, "Unauthorized")
                    }
                    if (response.status.value !in 200..299) throw ReceiptTransferException()
                    return@execute response.boundedBytes()
                }
                val location =
                    response.headers[HttpHeaders.Location] ?: throw ReceiptTransferException()
                val target =
                    try {
                        Url(location)
                    } catch (
                        _: IllegalArgumentException,
                    ) {
                        throw ReceiptTransferException()
                    }
                // Signed storage links must be absolute HTTPS URLs. Cookies are origin-scoped by PersistentSessionCookies.
                if (target.protocol != URLProtocol.HTTPS ||
                    target.host.isBlank() ||
                    !target.user.isNullOrEmpty() ||
                    !target.password.isNullOrEmpty()
                ) {
                    throw ReceiptTransferException()
                }
                client
                    .prepareGet(target) {
                        expectSuccess = false
                        timeout { requestTimeoutMillis = 120_000 }
                    }.execute { file ->
                        // Storage authorization failure is not evidence of an expired Pulse session. Never follow another redirect.
                        if (file.status.value !in 200..299) throw ReceiptTransferException()
                        file.boundedBytes()
                    }
            }
    }

    private suspend fun HttpResponse.boundedBytes(): ByteArray {
        if ((contentLength() ?: 0) > LIMIT) throw ReceiptTransferException()
        val stream = bodyAsChannel()
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        try {
            while (true) {
                val count = stream.readAvailable(buffer)
                if (count == -1) break
                if (output.size() + count > LIMIT) throw ReceiptTransferException()
                output.write(buffer, 0, count)
            }
            return output.toByteArray().also { if (it.isEmpty()) throw ReceiptTransferException() }
        } finally {
            stream.cancel()
        }
    }

    companion object {
        const val LIMIT = 10 * 1024 * 1024
    }
}
