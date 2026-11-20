package dev.abhishekan.pulse.di

import dev.abhishekan.pulse.appearance.AppearanceViewModel
import dev.abhishekan.pulse.core.ui.UnsavedChanges
import dev.abhishekan.pulse.session.SessionViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val applicationModule =
    module {
        singleOf(::UnsavedChanges)
        viewModelOf(::AppearanceViewModel)
        viewModelOf(::SessionViewModel)
    }
