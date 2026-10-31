package dev.abhishekan.pulse.di

import dev.abhishekan.pulse.AppearanceViewModel
import dev.abhishekan.pulse.SessionViewModel
import dev.abhishekan.pulse.core.domain.usecase.CheckSessionExpiry
import dev.abhishekan.pulse.core.domain.usecase.ForgetSession
import dev.abhishekan.pulse.core.domain.usecase.Logout
import dev.abhishekan.pulse.core.domain.usecase.ObserveFitnessChanges
import dev.abhishekan.pulse.core.domain.usecase.ObserveHorizonChanges
import dev.abhishekan.pulse.core.domain.usecase.ObserveSession
import dev.abhishekan.pulse.core.domain.usecase.ObserveTheme
import dev.abhishekan.pulse.core.domain.usecase.RestoreSession
import dev.abhishekan.pulse.core.domain.usecase.SetTheme
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val applicationModule =
    module {
        factoryOf(::ObserveHorizonChanges)
        factoryOf(::ObserveFitnessChanges)
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
