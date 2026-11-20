package dev.abhishekan.pulse.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

internal val Context.preferences by preferencesDataStore(name = "pulse_preferences")

class PulsePreferences(
    private val store: DataStore<Preferences>,
) : PreferencesRepository {
    override val theme =
        store.data
            .catch { error ->
                if (error is IOException) emit(emptyPreferences()) else throw error
            }.map { preferences ->
                ThemePreference.entries.firstOrNull { it.name == preferences[ThemeKey] }
                    ?: ThemePreference.System
            }

    override suspend fun setTheme(theme: ThemePreference): Outcome<Unit> =
        try {
            store.edit { it[ThemeKey] = theme.name }
            Outcome.Success(Unit)
        } catch (_: IOException) {
            Outcome.Error(Failure.Storage)
        }

    private companion object {
        val ThemeKey = stringPreferencesKey("theme")
    }
}
