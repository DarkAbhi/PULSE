package dev.abhishekan.pulse.feature.horizon.di
import dev.abhishekan.pulse.core.domain.usecase.ApplyHorizonChange
import dev.abhishekan.pulse.core.domain.usecase.GetHorizonTransactions
import dev.abhishekan.pulse.core.domain.usecase.GetPlannedPurchases
import dev.abhishekan.pulse.core.domain.usecase.GetSubscriptionPayments
import dev.abhishekan.pulse.core.domain.usecase.LoadHorizon
import dev.abhishekan.pulse.feature.horizon.HorizonViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val horizonModule =
    module {
        factoryOf(::LoadHorizon)
        factoryOf(::GetHorizonTransactions)
        factoryOf(::GetPlannedPurchases)
        factoryOf(::GetSubscriptionPayments)
        factoryOf(::ApplyHorizonChange)
        viewModelOf(::HorizonViewModel)
    }
