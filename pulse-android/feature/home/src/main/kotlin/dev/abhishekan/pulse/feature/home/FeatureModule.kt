package dev.abhishekan.pulse.feature.home

import dev.abhishekan.pulse.core.domain.*
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    factoryOf(::GetProfile)
    factoryOf(::GetWorkoutToday)
    factoryOf(::GetHorizon)
    factoryOf(::GetNotifications)
    viewModelOf(::HomeViewModel)
}
