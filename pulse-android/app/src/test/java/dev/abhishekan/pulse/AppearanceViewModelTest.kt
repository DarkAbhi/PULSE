package dev.abhishekan.pulse

import dev.abhishekan.pulse.core.domain.*
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppearanceViewModelTest {
    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun graphResolvesAndAppearanceRemainsObservable() = runTest {
        val preferences = object : PreferencesRepository {
            override val theme = MutableStateFlow(ThemePreference.System)
            override suspend fun setTheme(theme: ThemePreference) { this.theme.value = theme }
        }
        val app = koinApplication { modules(applicationModule, module { single<PreferencesRepository> { preferences } }) }
        try {
            val vm = app.koin.get<AppearanceViewModel>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
            vm.selectTheme(ThemePreference.Dark)
            runCurrent()
            assertEquals(ThemePreference.Dark, vm.uiState.value.theme)
            assertNull(vm.uiState.value.error)
        } finally { app.close() }
    }

    @Test fun persistenceFailureIsSafeAndDoesNotChangeTheme() = runTest {
        val preferences = object : PreferencesRepository {
            override val theme = MutableStateFlow(ThemePreference.System)
            override suspend fun setTheme(theme: ThemePreference) { throw IOException("sensitive disk path") }
        }
        val vm = AppearanceViewModel(ObserveTheme(preferences), SetTheme(preferences))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.selectTheme(ThemePreference.Dark)
        runCurrent()
        assertEquals(ThemePreference.System, vm.uiState.value.theme)
        assertEquals("Couldn’t save appearance. Please try again.", vm.uiState.value.error)
    }
}
