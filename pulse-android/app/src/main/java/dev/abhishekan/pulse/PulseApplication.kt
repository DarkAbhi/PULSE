package dev.abhishekan.pulse

import android.app.Application
import dev.abhishekan.pulse.core.network.NetworkConfig
import dev.abhishekan.pulse.di.pulseModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class PulseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PulseApplication)
            modules(
                pulseModules +
                    module {
                        single { NetworkConfig(BuildConfig.API_BASE_URL, BuildConfig.DEBUG) }
                    },
            )
        }
    }
}
