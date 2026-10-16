package dev.abhishekan.pulse.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.abhishekan.pulse.core.domain.ThemePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PulsePreferencesTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun defaultsAndExplicitThemeSurviveRepositoryRecreation() = runTest {
        val file = temporary.newFolder().resolve("theme.preferences_pb")
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + StandardTestDispatcher(testScheduler))) { file }
        val repository = PulsePreferences(store)
        assertEquals(ThemePreference.System, repository.theme.first())
        repository.setTheme(ThemePreference.Dark)
        job.cancelAndJoin()
        val reopened = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        assertEquals(ThemePreference.Dark, PulsePreferences(reopened).theme.first())
    }

    @Test fun unknownPreferenceFallsBackToSystem() = runTest {
        val file = temporary.newFolder().resolve("theme.preferences_pb")
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        store.edit { it[stringPreferencesKey("theme")] = "future-value" }
        assertEquals(ThemePreference.System, PulsePreferences(store).theme.first())
    }
}
