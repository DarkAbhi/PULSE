package dev.abhishekan.pulse.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.abhishekan.pulse.core.domain.AuthRepository
import dev.abhishekan.pulse.core.domain.DashboardRepository
import dev.abhishekan.pulse.core.network.SessionMaterialStore
import dev.abhishekan.pulse.core.domain.PreferencesRepository
import dev.abhishekan.pulse.core.domain.ThemePreference
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

private val Context.preferences by preferencesDataStore(name = "pulse_preferences")

class PulsePreferences(private val store: DataStore<Preferences>) : PreferencesRepository {
    override val theme = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { preferences ->
        ThemePreference.entries.firstOrNull { it.name == preferences[ThemeKey] } ?: ThemePreference.System
    }

    override suspend fun setTheme(theme: ThemePreference) {
        store.edit { it[ThemeKey] = theme.name }
    }

    private companion object { val ThemeKey = stringPreferencesKey("theme") }
}

val dataModule = module {
    single<DataStore<Preferences>> { get<Context>().preferences }
    singleOf(::PulsePreferences) bind PreferencesRepository::class
    singleOf(::AndroidSessionKey)
    singleOf(::EncryptedSessionStore) bind SessionMaterialStore::class
    singleOf(::PulseAuthRepository) bind AuthRepository::class
    singleOf(::PulseDashboardRepository) bind DashboardRepository::class
}
