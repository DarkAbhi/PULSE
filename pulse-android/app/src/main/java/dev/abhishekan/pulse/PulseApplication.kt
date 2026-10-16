package dev.abhishekan.pulse

import android.app.Application
import dev.abhishekan.pulse.core.data.di.dataModule
import dev.abhishekan.pulse.core.network.NetworkConfig
import dev.abhishekan.pulse.core.network.di.networkModule
import dev.abhishekan.pulse.di.applicationModule
import dev.abhishekan.pulse.feature.auth.di.authModule
import dev.abhishekan.pulse.feature.home.di.homeModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

val pulseModules = listOf(applicationModule, dataModule, networkModule, authModule, homeModule)

class PulseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PulseApplication)
            modules(pulseModules + module { single { NetworkConfig(BuildConfig.API_BASE_URL, BuildConfig.DEBUG) } })
        }
    }
}
