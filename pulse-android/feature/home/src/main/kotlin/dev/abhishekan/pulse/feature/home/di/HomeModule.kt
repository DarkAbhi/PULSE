package dev.abhishekan.pulse.feature.home.di

import dev.abhishekan.pulse.core.domain.usecase.GetHorizon
import dev.abhishekan.pulse.core.domain.usecase.GetProfile
import dev.abhishekan.pulse.core.domain.usecase.GetWorkoutToday
import dev.abhishekan.pulse.core.domain.usecase.ObserveDashboardChanges
import dev.abhishekan.pulse.feature.home.HomeViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule =
    module {
        factoryOf(::ObserveDashboardChanges)
        factoryOf(::GetProfile)
        factoryOf(::GetWorkoutToday)
        factoryOf(::GetHorizon)
        viewModelOf(::HomeViewModel)
    }
