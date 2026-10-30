package dev.abhishekan.pulse.feature.home

import androidx.lifecycle.ViewModelStore
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.domain.model.ExerciseInput
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.FitnessOverview
import dev.abhishekan.pulse.core.domain.model.Horizon
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import dev.abhishekan.pulse.core.domain.model.HorizonTransactionPage
import dev.abhishekan.pulse.core.domain.model.Notification
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.PlannedPurchases
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.WorkoutDetail
import dev.abhishekan.pulse.core.domain.model.WorkoutToday
import dev.abhishekan.pulse.core.domain.repository.DashboardRepository
import dev.abhishekan.pulse.core.domain.repository.FitnessRepository
import dev.abhishekan.pulse.core.domain.repository.HorizonRepository
import dev.abhishekan.pulse.core.domain.repository.NotificationsRepository
import dev.abhishekan.pulse.core.domain.usecase.GetHorizon
import dev.abhishekan.pulse.core.domain.usecase.GetNotificationCount
import dev.abhishekan.pulse.core.domain.usecase.GetProfile
import dev.abhishekan.pulse.core.domain.usecase.GetWorkoutToday
import dev.abhishekan.pulse.core.domain.usecase.ObserveDashboardChanges
import dev.abhishekan.pulse.core.domain.usecase.ObserveFitnessChanges
import dev.abhishekan.pulse.core.domain.usecase.ObserveHorizonChanges
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.currencyText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private var notificationResult: Outcome<List<Notification>> = Outcome.Success(emptyList())
    private val notifications =
        object : NotificationsRepository {
            override suspend fun list() = notificationResult

            override suspend fun dismiss(id: Long): Outcome<Unit> = error("Unused")

            override suspend fun clear(): Outcome<Unit> = error("Unused")
        }
    private val fitness =
        object : FitnessRepository {
            override val changes = MutableStateFlow(0L)

            override suspend fun overview(): Outcome<FitnessOverview> = error("Unused")

            override suspend fun workout(id: Long): Outcome<WorkoutDetail> = error("Unused")

            override suspend fun search(query: String): Outcome<List<CatalogExercise>> = error("Unused")

            override suspend fun record(date: LocalDate?): Outcome<Long> = error("Unused")

            override suspend fun saveExercise(
                visitId: Long,
                exerciseId: Long?,
                input: ExerciseInput,
            ): Outcome<Unit> = error("Unused")

            override suspend fun deleteWorkout(id: Long): Outcome<Unit> = error("Unused")

            override suspend fun deleteExercise(
                visitId: Long,
                exerciseId: Long,
            ): Outcome<Unit> = error("Unused")
        }

    private val horizon =
        object : HorizonRepository {
            override val changes = MutableStateFlow(0L)

            override suspend fun overview(): Outcome<HorizonOverview> = error("Unused")

            override suspend fun transactions(
                page: Int,
                search: String,
                type: String?,
            ): Outcome<HorizonTransactionPage> = error("Unused")

            override suspend fun purchases(): Outcome<PlannedPurchases> = error("Unused")

            override suspend fun payments(subscriptionId: Long): Outcome<List<HorizonTransaction>> = error("Unused")

            override suspend fun apply(change: HorizonChange): Outcome<Unit> = error("Unused")
        }

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun cleanup() {
        models.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    @Test fun sectionsLoadIndependentlyAndRetryOnlyTheFailedSection() =
        runTest {
            val profile = CompletableDeferred<Outcome<Profile>>()
            var profileCalls = 0
            var workoutCalls = 0
            var workoutRecorded = false
            var notificationCalls = 0
            var horizonFails = true
            val repository =
                object : DashboardRepository {
                    override suspend fun profile(): Outcome<Profile> {
                        profileCalls++
                        return profile.await()
                    }

                    override suspend fun workout(): Outcome<WorkoutToday> {
                        workoutCalls++
                        return Outcome.Success(WorkoutToday(workoutRecorded))
                    }

                    override suspend fun horizon(): Outcome<Horizon> =
                        if (horizonFails) {
                            Outcome.Error(Failure.Server)
                        } else {
                            Outcome.Success(
                                Horizon("₹", BigDecimal("100.00"), BigDecimal("12.50"), BigDecimal("87.50")),
                            )
                        }

                    override suspend fun notifications(): Outcome<List<Notification>> {
                        notificationCalls++
                        return Outcome.Success(listOf(Notification(1, "An update", null, null)))
                    }
                }
            val vm =
                HomeViewModel(
                    GetProfile(repository),
                    GetWorkoutToday(repository),
                    GetHorizon(repository),
                    ObserveFitnessChanges(fitness),
                    ObserveHorizonChanges(horizon),
                    ObserveDashboardChanges(repository),
                    GetNotificationCount(notifications),
                )
            models.put("home", vm)
            runCurrent()
            assertEquals(Section.Loading, vm.uiState.value.profile)
            assertEquals(Section.Empty, vm.uiState.value.workout)
            assertEquals(Section.Error(Failure.Server), vm.uiState.value.horizon)
            vm.retryProfile()
            runCurrent()
            assertEquals(1, profileCalls)
            horizonFails = false
            vm.retryHorizon()
            runCurrent()
            assertTrue(vm.uiState.value.horizon is Section.Content)
            assertEquals(0, notificationCalls)
            // The badge uses the complete notification center, not the five-item dashboard preview.
            notificationResult = Outcome.Success(List(12) { Notification(it.toLong() + 1, "Update", null, null) })
            vm.refreshNotificationCount()
            runCurrent()
            assertEquals(12, vm.uiState.value.notificationCount)
            notificationResult = Outcome.Error(Failure.Connection)
            vm.refreshNotificationCount()
            runCurrent()
            assertEquals(12, vm.uiState.value.notificationCount)
            notificationResult = Outcome.Success(listOf(Notification(1, "Update", null, null)))
            vm.refreshNotificationCount()
            runCurrent()
            assertEquals(1, vm.uiState.value.notificationCount)
            notificationResult = Outcome.Success(emptyList())
            vm.refreshNotificationCount()
            runCurrent()
            assertEquals(0, vm.uiState.value.notificationCount)
            profile.complete(Outcome.Success(Profile(null)))
            runCurrent()
            assertEquals(Section.Empty, vm.uiState.value.profile)
            workoutRecorded = true
            fitness.changes.value++
            runCurrent()
            assertEquals(Section.Content(WorkoutToday(true)), vm.uiState.value.workout)
            assertEquals(2, workoutCalls)
            horizonFails = true
            horizon.changes.value++
            runCurrent()
            assertTrue(vm.uiState.value.horizon is Section.Content)
            assertEquals(Failure.Server, vm.uiState.value.refreshErrors["horizon"])
            assertEquals(2, workoutCalls)
            vm.retryWorkout()
            assertEquals(Section.Content(WorkoutToday(true)), vm.uiState.value.workout)
            assertTrue("workout" in vm.uiState.value.refreshing)
            runCurrent()
            assertTrue("workout" !in vm.uiState.value.refreshing)
            assertEquals(3, workoutCalls)
            assertEquals(0, notificationCalls)
        }

    @Test fun dashboardClockUsesUtcByDefault() {
        assertEquals(
            "Saturday, 17 Oct 2026",
            formatDashboardDate(Instant.parse("2026-10-17T18:30:00Z"), Locale.US),
        )
        assertEquals(
            "Sunday, 18 Oct 2026",
            formatDashboardDate(Instant.parse("2026-10-18T07:35:00Z"), Locale.US),
        )
    }

    @Test fun dashboardTimeUsesUtcByDefault() {
        assertEquals("6:30 PM", formatDashboardTime(Instant.parse("2026-10-17T18:30:00Z"), Locale.US))
        assertEquals("7:35 AM", formatDashboardTime(Instant.parse("2026-10-18T07:35:00Z"), Locale.US))
    }

    @Test fun currencyFormattingDoesNotRoundBackendPrecision() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            UserCurrency
                .select("USD")
            assertEquals(
                UiText
                    .Literal("\$12.50"),
                currencyText(BigDecimal("12.5")),
            )
            assertEquals(
                UiText
                    .Literal("-\$0.000000000000123456789"),
                currencyText(BigDecimal("-0.000000000000123456789")),
            )
        } finally {
            UserCurrency
                .reset()
            Locale.setDefault(previous)
        }
    }
}
