package dev.abhishekan.pulse

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dev.abhishekan.pulse.core.network.NetworkConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import org.junit.Test
import org.koin.dsl.module
import org.koin.test.verify.definition
import org.koin.test.verify.injectedParameters
import org.koin.test.verify.verify

@OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)
class KoinGraphTest {
    @Test fun completeProductionGraphHasEveryConstructorDependency() {
        module {
            includes(pulseModules)
            single { NetworkConfig("https://example.com/api/", false) }
        }.verify(
            // Android and Navigation supply these at runtime.
            extraTypes = listOf(Context::class, SavedStateHandle::class),
            // Factory literals and the HTTP factory's internally constructed engine aren't DI dependencies.
            injections =
                injectedParameters(
                    definition<NetworkConfig>(String::class, Boolean::class),
                    definition<HttpClient>(HttpClientEngine::class),
                ),
        )
    }
}
