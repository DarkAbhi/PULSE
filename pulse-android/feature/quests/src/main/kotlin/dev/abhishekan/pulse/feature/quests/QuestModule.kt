package dev.abhishekan.pulse.feature.quests

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val questModule =
    module {
        viewModelOf(::QuestViewModel)
    }
