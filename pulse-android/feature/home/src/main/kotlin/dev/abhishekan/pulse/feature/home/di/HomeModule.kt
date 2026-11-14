package dev.abhishekan.pulse.feature.home.di

import dev.abhishekan.pulse.feature.home.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule =
    module {
        viewModelOf(::HomeViewModel)
    }
