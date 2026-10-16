package dev.abhishekan.pulse.feature.auth

import dev.abhishekan.pulse.core.domain.Login
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authModule = module {
    factoryOf(::Login)
    viewModelOf(::LoginViewModel)
}
