package dev.abhishekan.pulse
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.domain.repository.PreferencesRepository
import dev.abhishekan.pulse.core.domain.usecase.ObserveTheme
import dev.abhishekan.pulse.core.domain.usecase.SetTheme
import dev.abhishekan.pulse.di.applicationModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppearanceViewModelTest {
    @Before fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun graphResolvesAndAppearanceRemainsObservable() =
        runTest {
            val preferences =
                object : PreferencesRepository {
                    override val theme = MutableStateFlow(ThemePreference.System)

                    override suspend fun setTheme(theme: ThemePreference): Outcome<Unit> {
                        this.theme.value = theme
                        return Outcome.Success(Unit)
                    }
                }
            val app =
                koinApplication {
                    modules(applicationModule, module { single<PreferencesRepository> { preferences } })
                }
            try {
                val vm = app.koin.get<AppearanceViewModel>()
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.uiState.collect {}
                }
                vm.selectTheme(ThemePreference.Dark)
                runCurrent()
                assertEquals(ThemePreference.Dark, vm.uiState.value.theme)
                assertNull(vm.uiState.value.error)
            } finally {
                app.close()
            }
        }

    @Test fun persistenceFailureIsSafeAndDoesNotChangeTheme() =
        runTest {
            val preferences =
                object : PreferencesRepository {
                    override val theme = MutableStateFlow(ThemePreference.System)

                    override suspend fun setTheme(theme: ThemePreference) = Outcome.Error(Failure.Storage)
                }
            val vm = AppearanceViewModel(ObserveTheme(preferences), SetTheme(preferences))
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
            vm.selectTheme(ThemePreference.Dark)
            runCurrent()
            assertEquals(ThemePreference.System, vm.uiState.value.theme)
            assertEquals(
                dev.abhishekan.pulse.core.ui.uiText(
                    R.string.pulse_app_couldn_t_save_appearance_please_try_again,
                ),
                vm.uiState.value.error,
            )
        }
}
