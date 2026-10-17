package dev.abhishekan.pulse.feature.garage.di

import dev.abhishekan.pulse.core.domain.usecase.*
import dev.abhishekan.pulse.feature.garage.GarageViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val garageModule = module {
    factoryOf(::GetVehicles)
    factoryOf(::GetVehicleHistory)
    factoryOf(::ApplyGarageChange)
    factoryOf(::UploadGarageReceipt)
    factoryOf(::DownloadGarageReceipt)
    viewModelOf(::GarageViewModel)
}
