package dev.abhishekan.pulse.core.domain.repository

import dev.abhishekan.pulse.core.domain.model.MealOnboarding
import dev.abhishekan.pulse.core.domain.model.MealPreferences
import dev.abhishekan.pulse.core.domain.model.Outcome

interface MealPreferencesRepository {
    suspend fun onboarding(): Outcome<MealOnboarding>

    suspend fun resolve(preferences: MealPreferences?): Outcome<MealOnboarding>

    suspend fun save(preferences: MealPreferences): Outcome<MealOnboarding>
}
