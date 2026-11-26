package dev.abhishekan.pulse.di

import dev.abhishekan.pulse.core.data.di.dataModule
import dev.abhishekan.pulse.core.network.di.networkModule
import dev.abhishekan.pulse.feature.auth.di.authModule
import dev.abhishekan.pulse.feature.fitness.di.fitnessModule
import dev.abhishekan.pulse.feature.garage.di.garageModule
import dev.abhishekan.pulse.feature.home.di.homeModule
import dev.abhishekan.pulse.feature.horizon.di.horizonModule
import dev.abhishekan.pulse.feature.mealplan.di.mealPlanModule
import dev.abhishekan.pulse.feature.notifications.di.notificationsModule
import dev.abhishekan.pulse.feature.profile.di.profileModule
import dev.abhishekan.pulse.feature.quests.questModule

internal val pulseModules =
    listOf(
        applicationModule,
        dataModule,
        networkModule,
        authModule,
        homeModule,
        questModule,
        fitnessModule,
        horizonModule,
        garageModule,
        mealPlanModule,
        profileModule,
        notificationsModule,
    )
