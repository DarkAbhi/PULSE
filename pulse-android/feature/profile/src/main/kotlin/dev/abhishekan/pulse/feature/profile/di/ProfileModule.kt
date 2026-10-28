package dev.abhishekan.pulse.feature.profile.di
import dev.abhishekan.pulse.core.domain.usecase.ChangePassword
import dev.abhishekan.pulse.core.domain.usecase.CreateAPIKey
import dev.abhishekan.pulse.core.domain.usecase.GetAPIKeys
import dev.abhishekan.pulse.core.domain.usecase.GetAccountProfile
import dev.abhishekan.pulse.core.domain.usecase.RevokeAPIKey
import dev.abhishekan.pulse.core.domain.usecase.SaveProfileName
import dev.abhishekan.pulse.core.domain.usecase.SaveProfileTimeZone
import dev.abhishekan.pulse.feature.profile.ProfileViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule =
    module {
        factoryOf(::GetAccountProfile)
        factoryOf(::SaveProfileName)
        factoryOf(::SaveProfileTimeZone)
        factoryOf(::ChangePassword)
        factoryOf(::GetAPIKeys)
        factoryOf(::CreateAPIKey)
        factoryOf(::RevokeAPIKey)
        viewModelOf(::ProfileViewModel)
    }
