package dev.abhishekan.pulse.core.data
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class PulsePreferencesTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun defaultsAndExplicitThemeSurviveRepositoryRecreation() =
        runTest {
            val file = temporary.newFolder().resolve("theme.preferences_pb")
            val job = SupervisorJob()
            val store =
                PreferenceDataStoreFactory.create(
                    scope =
                        CoroutineScope(
                            job + StandardTestDispatcher(testScheduler),
                        ),
                ) { file }
            val repository = PulsePreferences(store)
            assertEquals(ThemePreference.System, repository.theme.first())
            assertEquals(Outcome.Success(Unit), repository.setTheme(ThemePreference.Dark))
            job.cancelAndJoin()
            val reopened = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
            assertEquals(ThemePreference.Dark, PulsePreferences(reopened).theme.first())
        }

    @Test fun unknownPreferenceFallsBackToSystem() =
        runTest {
            val file = temporary.newFolder().resolve("theme.preferences_pb")
            val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
            store.edit { it[stringPreferencesKey("theme")] = "future-value" }
            assertEquals(ThemePreference.System, PulsePreferences(store).theme.first())
        }

    @Test fun writeFailuresBecomeDomainOutcomesAndCancellationPropagates() =
        runTest {
            var failure: Exception = IOException("private disk path")
            val store =
                object : DataStore<Preferences> {
                    override val data = flowOf(emptyPreferences())

                    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = throw failure
                }
            val repository = PulsePreferences(store)
            assertEquals(Outcome.Error(Failure.Storage), repository.setTheme(ThemePreference.Dark))
            failure = CancellationException("Cancelled")
            try {
                repository.setTheme(ThemePreference.Light)
                fail("Cancellation must propagate")
            } catch (_: CancellationException) {
            }
        }
}
