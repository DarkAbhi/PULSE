package dev.abhishekan.pulse.core.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.SendingRequest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.parseClientCookiesHeader
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json

internal val SignedStorageRequest = AttributeKey<Unit>("PulseSignedStorageRequest")

fun pulseHttpClient(
    config: NetworkConfig,
    engine: HttpClientEngine = OkHttp.create(),
    cookies: CookiesStorage = AcceptAllCookiesStorage(),
    logger: (String) -> Unit = { Log.d("PulseNetwork", it) },
): HttpClient =
    HttpClient(engine) {
        install(HttpCookies) { storage = cookies }
        install(
            createClientPlugin("PulseSessionBearer") {
                val api = Url(config.baseUrl)
                on(SendingRequest) { request, _ ->
                    val target = request.url.build()
                    val isApi =
                        target.protocol == api.protocol &&
                            target.host == api.host &&
                            target.port == api.port &&
                            target.encodedPath.startsWith(api.encodedPath) &&
                            !request.attributes.contains(SignedStorageRequest)
                    request.headers.remove(HttpHeaders.Authorization)
                    request.headers.remove("X-Session-Token")
                    if (isApi) {
                        // Use the cookie selected for this request so an account switch cannot mix credentials.
                        val token = request.headers[HttpHeaders.Cookie]?.let { parseClientCookiesHeader(it)["life_session"] }
                        token?.let { request.headers[HttpHeaders.Authorization] = "Bearer $it" }
                        request.headers[HttpHeaders.CacheControl] = "no-store"
                    } else {
                        // Signed receipt links may share the API origin; never send session credentials to storage.
                        request.headers.remove(HttpHeaders.Cookie)
                    }
                }
            },
        )
        expectSuccess = true
        followRedirects = false
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis =
                15_000
        }
        defaultRequest {
            url(config.baseUrl)
            contentType(ContentType.Application.Json)
        }
        if (config.debug) {
            install(Logging) {
                this.logger =
                    object : Logger {
                        override fun log(message: String) {
                            logger(message)
                        }
                    }
                level = LogLevel.INFO
                val api = Url(config.baseUrl)
                // Signed storage URLs must never appear in logs.
                filter {
                    !it.attributes.contains(SignedStorageRequest) &&
                        it.url.protocol == api.protocol &&
                        it.url.host == api.host &&
                        it.url.port == api.port &&
                        it.url.encodedPath.startsWith(api.encodedPath)
                }
            }
        }
        engine {
            if (this is OkHttpConfig) {
                config {
                    retryOnConnectionFailure(false)
                }
            }
        }
    }
