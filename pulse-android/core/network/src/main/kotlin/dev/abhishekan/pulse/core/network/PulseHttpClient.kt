package dev.abhishekan.pulse.core.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json

internal val SkipHttpLogging = AttributeKey<Unit>("PulseSkipHttpLogging")

fun pulseHttpClient(
    config: NetworkConfig,
    engine: HttpClientEngine = OkHttp.create(),
    cookies: CookiesStorage = AcceptAllCookiesStorage(),
    logger: (String) -> Unit = { Log.d("PulseNetwork", it) },
): HttpClient =
    HttpClient(engine) {
        install(HttpCookies) { storage = cookies }
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
                    !it.attributes.contains(SkipHttpLogging) &&
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
