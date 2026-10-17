package dev.abhishekan.pulse.feature.horizon.di
import dev.abhishekan.pulse.feature.horizon.HorizonViewModel
import dev.abhishekan.pulse.core.domain.usecase.*
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
val horizonModule = module {
    factoryOf(::LoadHorizon)
    factoryOf(::GetHorizonTransactions)
    factoryOf(::GetPlannedPurchases)
    factoryOf(::GetSubscriptionPayments)
    factoryOf(::ApplyHorizonChange)
    viewModelOf(::HorizonViewModel)
}
