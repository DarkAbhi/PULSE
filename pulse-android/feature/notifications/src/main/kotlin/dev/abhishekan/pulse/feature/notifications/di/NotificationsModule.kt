package dev.abhishekan.pulse.feature.notifications.di
import dev.abhishekan.pulse.core.domain.usecase.DismissNotification
import dev.abhishekan.pulse.feature.notifications.NotificationsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val notificationsModule =
    module {

        factoryOf(::DismissNotification)

        viewModelOf(::NotificationsViewModel)
    }
