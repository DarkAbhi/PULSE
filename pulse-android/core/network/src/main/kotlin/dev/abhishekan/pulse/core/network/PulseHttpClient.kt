package dev.abhishekan.pulse.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun pulseHttpClient(
    config: NetworkConfig,
    engine: HttpClientEngine = OkHttp.create(),
    cookies: CookiesStorage = AcceptAllCookiesStorage(),
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
        // No logging or automatic retries: credentials must never appear in logs.
        engine {
            if (this is io.ktor.client.engine.okhttp.OkHttpConfig) {
                config {
                    retryOnConnectionFailure(false)
                }
            }
        }
    }
