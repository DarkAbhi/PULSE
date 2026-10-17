package dev.abhishekan.pulse.core.network.di

import dev.abhishekan.pulse.core.network.MealPlanApi
import dev.abhishekan.pulse.core.network.GarageApi
import dev.abhishekan.pulse.core.network.HorizonApi
import dev.abhishekan.pulse.core.network.FitnessApi
import dev.abhishekan.pulse.core.network.PersistentSessionCookies
import dev.abhishekan.pulse.core.network.PulseApi
import dev.abhishekan.pulse.core.network.pulseHttpClient
import java.time.Clock
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.dsl.onClose

val networkModule = module {
    singleOf(::MealPlanApi)
    singleOf(::GarageApi)
    singleOf(::HorizonApi)
    singleOf(::FitnessApi)
    single<Clock> { Clock.systemUTC() }
    singleOf(::PersistentSessionCookies)
    singleOf(::PulseApi)
    single { pulseHttpClient(get(), cookies = get<PersistentSessionCookies>()) } onClose { it?.close() }
}
