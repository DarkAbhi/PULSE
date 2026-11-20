package dev.abhishekan.pulse.feature.auth.di

import dev.abhishekan.pulse.core.domain.usecase.Login
import dev.abhishekan.pulse.feature.auth.LoginViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authModule =
    module {
        factoryOf(::Login)
        viewModelOf(::LoginViewModel)
    }
