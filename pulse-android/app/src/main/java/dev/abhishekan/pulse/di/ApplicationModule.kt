package dev.abhishekan.pulse.di

import dev.abhishekan.pulse.AppearanceViewModel
import dev.abhishekan.pulse.SessionViewModel
import dev.abhishekan.pulse.core.ui.UnsavedChanges
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val applicationModule =
    module {
        singleOf(::UnsavedChanges)
        viewModelOf(::AppearanceViewModel)
        viewModelOf(::SessionViewModel)
    }
