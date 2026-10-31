package dev.abhishekan.pulse.feature.garage.di
import dev.abhishekan.pulse.core.domain.usecase.ApplyGarageChange
import dev.abhishekan.pulse.core.domain.usecase.DownloadGarageReceipt
import dev.abhishekan.pulse.core.domain.usecase.GetVehicleHistory
import dev.abhishekan.pulse.core.domain.usecase.GetVehicles
import dev.abhishekan.pulse.core.domain.usecase.SearchVehicleCatalog
import dev.abhishekan.pulse.core.domain.usecase.UploadGarageReceipt
import dev.abhishekan.pulse.feature.garage.GarageViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val garageModule =
    module {
        factoryOf(::GetVehicles)
        factoryOf(::SearchVehicleCatalog)
        factoryOf(::GetVehicleHistory)
        factoryOf(::ApplyGarageChange)
        factoryOf(::UploadGarageReceipt)
        factoryOf(::DownloadGarageReceipt)
        viewModelOf(::GarageViewModel)
    }
