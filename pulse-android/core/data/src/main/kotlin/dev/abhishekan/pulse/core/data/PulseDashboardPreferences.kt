package dev.abhishekan.pulse.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.DashboardPreferencesRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class PulseDashboardPreferences(
    private val store: DataStore<Preferences>,
) : DashboardPreferencesRepository {
    override val compact =
        store.data
            .catch { error ->
                if (error is IOException) emit(emptyPreferences()) else throw error
            }.map { it[CompactDashboardKey] ?: false }

    override suspend fun setCompact(compact: Boolean): Outcome<Unit> =
        try {
            store.edit { it[CompactDashboardKey] = compact }
            Outcome.Success(Unit)
        } catch (_: IOException) {
            Outcome.Error(Failure.Storage)
        }

    private companion object {
        val CompactDashboardKey = booleanPreferencesKey("compact_dashboard")
    }
}
