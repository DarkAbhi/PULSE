package dev.abhishekan.pulse.feature.mealplan.di
import dev.abhishekan.pulse.core.domain.usecase.ApplyMealChange
import dev.abhishekan.pulse.core.domain.usecase.GetMealTimes
import dev.abhishekan.pulse.core.domain.usecase.GetMealWeek
import dev.abhishekan.pulse.core.domain.usecase.MealCalendar
import dev.abhishekan.pulse.core.domain.usecase.SetMealConsumed
import dev.abhishekan.pulse.feature.mealplan.MealPlanViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val mealPlanModule =
    module {
        factoryOf(::GetMealWeek)
        factoryOf(::GetMealTimes)
        factoryOf(::ApplyMealChange)
        factoryOf(::SetMealConsumed)
        factoryOf(::MealCalendar)
        viewModelOf(::MealPlanViewModel)
    }
