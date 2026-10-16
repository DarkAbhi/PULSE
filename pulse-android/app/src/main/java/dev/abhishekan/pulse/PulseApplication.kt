package dev.abhishekan.pulse

import android.app.Application
import dev.abhishekan.pulse.core.data.dataModule
import dev.abhishekan.pulse.core.domain.*
import dev.abhishekan.pulse.core.domain.ObserveTheme
import dev.abhishekan.pulse.core.domain.SetTheme
import dev.abhishekan.pulse.core.network.NetworkConfig
import dev.abhishekan.pulse.core.network.networkModule
import dev.abhishekan.pulse.feature.auth.authModule
import dev.abhishekan.pulse.feature.home.homeModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.context.startKoin
import org.koin.dsl.module

val applicationModule = module {
    factoryOf(::ObserveTheme)
    factoryOf(::SetTheme)
    viewModelOf(::AppearanceViewModel)
    factoryOf(::ObserveSession)
    factoryOf(::RestoreSession)
    factoryOf(::Logout)
    factoryOf(::ForgetSession)
    factoryOf(::CheckSessionExpiry)
    viewModelOf(::SessionViewModel)
}

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
