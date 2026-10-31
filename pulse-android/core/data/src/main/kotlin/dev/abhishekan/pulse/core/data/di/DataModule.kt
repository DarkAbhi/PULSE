package dev.abhishekan.pulse.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.abhishekan.pulse.core.data.AndroidReceiptFiles
import dev.abhishekan.pulse.core.data.AndroidSessionKey
import dev.abhishekan.pulse.core.data.EncryptedSessionStore
import dev.abhishekan.pulse.core.data.PulseAuthRepository
import dev.abhishekan.pulse.core.data.PulseDashboardRepository
import dev.abhishekan.pulse.core.data.PulseFitnessRepository
import dev.abhishekan.pulse.core.data.PulseGarageRepository
import dev.abhishekan.pulse.core.data.PulseHorizonRepository
import dev.abhishekan.pulse.core.data.PulseMealPlanRepository
import dev.abhishekan.pulse.core.data.PulseNotificationsRepository
import dev.abhishekan.pulse.core.data.PulsePreferences
import dev.abhishekan.pulse.core.data.PulseProfileRepository
import dev.abhishekan.pulse.core.data.ReceiptFiles
import dev.abhishekan.pulse.core.data.preferences
import dev.abhishekan.pulse.core.domain.repository.AuthRepository
import dev.abhishekan.pulse.core.domain.repository.DashboardRepository
import dev.abhishekan.pulse.core.domain.repository.FitnessRepository
import dev.abhishekan.pulse.core.domain.repository.GarageRepository
import dev.abhishekan.pulse.core.domain.repository.HorizonRepository
import dev.abhishekan.pulse.core.domain.repository.MealPlanRepository
import dev.abhishekan.pulse.core.domain.repository.NotificationsRepository
import dev.abhishekan.pulse.core.domain.repository.PreferencesRepository
import dev.abhishekan.pulse.core.domain.repository.ProfileRepository
import dev.abhishekan.pulse.core.domain.repository.VehicleCatalogRepository
import dev.abhishekan.pulse.core.network.SessionMaterialStore
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule =
    module {
        singleOf(::PulseProfileRepository) bind ProfileRepository::class
        singleOf(::PulseNotificationsRepository) bind NotificationsRepository::class
        singleOf(::PulseMealPlanRepository) bind
            MealPlanRepository::class
        singleOf(::AndroidReceiptFiles) bind ReceiptFiles::class
        singleOf(::PulseGarageRepository) bind
            GarageRepository::class
        single<VehicleCatalogRepository> { get<PulseGarageRepository>() }
        singleOf(::PulseHorizonRepository) bind HorizonRepository::class
        singleOf(::PulseFitnessRepository) bind FitnessRepository::class
        single<DataStore<Preferences>> { get<Context>().preferences }
        singleOf(::PulsePreferences) bind PreferencesRepository::class
        singleOf(::AndroidSessionKey)
        singleOf(::EncryptedSessionStore) bind SessionMaterialStore::class
        singleOf(::PulseAuthRepository) bind AuthRepository::class
        singleOf(::PulseDashboardRepository) bind DashboardRepository::class
    }
