package dev.abhishekan.pulse.feature.home

import androidx.lifecycle.ViewModelStore
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Horizon
import dev.abhishekan.pulse.core.domain.model.Notification
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.domain.model.WorkoutToday
import dev.abhishekan.pulse.core.domain.repository.DashboardRepository
import dev.abhishekan.pulse.core.domain.usecase.GetHorizon
import dev.abhishekan.pulse.core.domain.usecase.GetNotifications
import dev.abhishekan.pulse.core.domain.usecase.GetProfile
import dev.abhishekan.pulse.core.domain.usecase.GetWorkoutToday
import java.math.BigDecimal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { models.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }

    @Test fun sectionsLoadIndependentlyAndRetryOnlyTheFailedSection() = runTest {
        val profile = CompletableDeferred<Outcome<Profile>>()
        var profileCalls = 0; var notificationCalls = 0; var horizonFails = true
        val repository = object : DashboardRepository {
            override suspend fun profile(): Outcome<Profile> { profileCalls++; return profile.await() }
            override suspend fun workout() = Outcome.Success(WorkoutToday(false))
            override suspend fun horizon(): Outcome<Horizon> = if (horizonFails) Outcome.Error(Failure.Server)
                else Outcome.Success(Horizon("₹", BigDecimal("100.00"), BigDecimal("12.50"), BigDecimal("87.50")))
            override suspend fun notifications(): Outcome<List<Notification>> {
                notificationCalls++; return Outcome.Success(listOf(Notification(1, "An update", null, null)))
            }
        }
        val vm = HomeViewModel(GetProfile(repository), GetWorkoutToday(repository), GetHorizon(repository), GetNotifications(repository))
        models.put("home", vm)
        runCurrent()
        assertEquals(Section.Loading, vm.uiState.value.profile)
        assertEquals(Section.Empty, vm.uiState.value.workout)
        assertEquals(Section.Error(Failure.Server), vm.uiState.value.horizon)
        assertTrue(vm.uiState.value.notifications is Section.Content)
        vm.retryProfile(); runCurrent(); assertEquals(1, profileCalls)
        horizonFails = false; vm.retryHorizon(); runCurrent()
        assertTrue(vm.uiState.value.horizon is Section.Content)
        assertEquals(1, notificationCalls)
        profile.complete(Outcome.Success(Profile(null))); runCurrent()
        assertEquals(Section.Empty, vm.uiState.value.profile)
    }

    @Test fun currencyFormattingDoesNotRoundBackendPrecision() {
        assertEquals("₹ 12.50", formatMoney("₹", BigDecimal("12.5")))
        assertEquals("USD -0.000000000000123456789", formatMoney("USD", BigDecimal("-0.000000000000123456789")))
    }
}
