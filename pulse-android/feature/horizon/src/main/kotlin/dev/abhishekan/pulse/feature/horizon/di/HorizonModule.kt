package dev.abhishekan.pulse.feature.horizon.di

import dev.abhishekan.pulse.core.domain.usecase.ApplyHorizonChange
import dev.abhishekan.pulse.core.domain.usecase.GetHorizonTransactions
import dev.abhishekan.pulse.core.domain.usecase.GetSubscriptionPayments
import dev.abhishekan.pulse.feature.horizon.presentation.accounts.AccountsViewModel
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonViewModel
import dev.abhishekan.pulse.feature.horizon.presentation.statementimport.StatementImportViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val horizonModule =
    module {

        factoryOf(::GetHorizonTransactions)

        factoryOf(::GetSubscriptionPayments)
        factoryOf(::ApplyHorizonChange)
        viewModelOf(::HorizonViewModel)
        viewModelOf(::AccountsViewModel)
        viewModelOf(::StatementImportViewModel)
    }
