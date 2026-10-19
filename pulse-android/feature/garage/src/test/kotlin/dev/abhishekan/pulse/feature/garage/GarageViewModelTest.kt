package dev.abhishekan.pulse.feature.garage
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.FuelFill
import dev.abhishekan.pulse.core.domain.model.FuelItem
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.domain.model.GarageRecord
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.TirePressure
import dev.abhishekan.pulse.core.domain.model.Vehicle
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.core.domain.model.garageCategories
import dev.abhishekan.pulse.core.domain.repository.GarageRepository
import dev.abhishekan.pulse.core.domain.usecase.ApplyGarageChange
import dev.abhishekan.pulse.core.domain.usecase.DownloadGarageReceipt
import dev.abhishekan.pulse.core.domain.usecase.GetVehicleHistory
import dev.abhishekan.pulse.core.domain.usecase.GetVehicles
import dev.abhishekan.pulse.core.domain.usecase.UploadGarageReceipt
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class GarageViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()

    private class Repo : GarageRepository {
        override val changes = MutableStateFlow(0L)
        var vehicles = listOf(Vehicle(1, "Ride", "two_wheeler", listOf("petrol")))
        var failure: Failure? = null
        var reads = 0
        val writes = mutableListOf<GarageChange>()
        var write: suspend () -> Outcome<Unit> = { Outcome.Success(Unit) }
        var uploadResult: Outcome<Unit> = Outcome.Success(Unit)
        val history =
            VehicleHistory(
                "Ride",
                TirePressure(),
                emptyList(),
                listOf(
                    FuelFill(
                        2,
                        "100".toBigDecimal(),
                        Instant.EPOCH,
                        null,
                        null,
                        listOf(
                            FuelItem(
                                "petrol",
                                "full",
                                "2".toBigDecimal(),
                                "100".toBigDecimal(),
                                "200".toBigDecimal(),
                            ),
                        ),
                    ),
                ),
                emptyList(),
                emptyMap(),
                "two_wheeler",
                listOf("petrol"),
            )

        override suspend fun vehicles() = Outcome.Success(vehicles)

        override suspend fun history(vehicle: Long): Outcome<VehicleHistory> {
            reads++
            return failure?.let { Outcome.Error(it) }
                ?: Outcome.Success(history)
        }

        override suspend fun apply(change: GarageChange): Outcome<Unit> {
            writes += change
            return write()
        }

        override suspend fun upload(
            vehicle: Long,
            record: Long,
            source: String,
            progress: (Float) -> Unit,
        ): Outcome<Unit> {
            progress(.5f)
            return uploadResult
        }

        override suspend fun download(
            vehicle: Long,
            record: Long,
            receipt: GarageReceipt,
            destination: String?,
        ) = Outcome.Success("content://preview/receipt")
    }

    private val repo = Repo()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun cleanup() {
        models.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    private fun vm(saved: SavedStateHandle = SavedStateHandle()) =
        GarageViewModel(
            GetVehicles(repo),
            GetVehicleHistory(repo),
            ApplyGarageChange(repo),
            UploadGarageReceipt(repo),
            DownloadGarageReceipt(repo),
            saved,
        ).also {
            models.put("garage", it)
        }

    @Test fun refreshKeepsVehicleAndHistoryDataIncludingOnFailure() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.select(1)
            runCurrent()
            val previous = vm.uiState.value
            repo.failure = Failure.Connection
            vm.refresh()
            assertTrue(vm.uiState.value.vehicles.loading)
            assertTrue(vm.uiState.value.history.loading)
            assertEquals(previous.vehicles.value, vm.uiState.value.vehicles.value)
            assertEquals(previous.history.value, vm.uiState.value.history.value)
            runCurrent()
            assertEquals(Failure.Connection, vm.uiState.value.history.error)
            assertEquals(previous.history.value, vm.uiState.value.history.value)
            repo.vehicles = repo.vehicles + Vehicle(2, "Other", "two_wheeler", listOf("petrol"))
            vm.select(2)
            assertNull(vm.uiState.value.history.value)
            runCurrent()
        }

    @Test fun missingSelectedVehicleReturnsToCollectionWithFeedbackEvenWhenListFinishesFirst() =
        runTest {
            repo.vehicles = emptyList()
            val vm = vm(SavedStateHandle(mapOf("vehicle" to 999L)))
            runCurrent()
            assertNull(vm.uiState.value.vehicle)
            assertEquals(Failure.NotFound.message, vm.uiState.value.message)
        }

    @Test fun historyFailureKeepsCollectionAndRetries() =
        runTest {
            val vm = vm()
            runCurrent()
            repo.failure = Failure.Connection
            vm.select(1)
            runCurrent()
            assertEquals(
                1,
                vm.uiState.value.vehicles.value
                    ?.size,
            )
            assertEquals(Failure.Connection, vm.uiState.value.history.error)
            repo.failure = null
            vm.refreshHistory()
            runCurrent()
            assertNotNull(vm.uiState.value.history.value)
        }

    @Test fun duplicateWritesBlockedAndRefreshAfterSuccess() =
        runTest {
            val gate = CompletableDeferred<Outcome<Unit>>()
            repo.write = { gate.await() }
            val vm = vm()
            runCurrent()
            vm.select(1)
            runCurrent()
            vm.mutate(GarageChange.Air(1))
            vm.mutate(GarageChange.Air(1))
            runCurrent()
            assertEquals(1, repo.writes.size)
            gate.complete(Outcome.Success(Unit))
            runCurrent()
            assertEquals(2, repo.reads)
            assertFalse(vm.uiState.value.busy)
        }

    @Test fun failedSaveRequiresRefreshedRecordsAndExplicitReview() =
        runTest {
            repo.write = {
                repo.failure = Failure.Connection
                Outcome.Error(Failure.Timeout)
            }
            val vm = vm()
            runCurrent()
            vm.select(1)
            runCurrent()
            vm.mutate(GarageChange.Air(1))
            runCurrent()
            assertTrue(vm.uiState.value.reviewRequired)
            vm.acknowledgeReview()
            vm.mutate(GarageChange.Air(1))
            runCurrent()
            assertEquals(1, repo.writes.size)
            repo.failure = null
            vm.refresh()
            runCurrent()
            vm.acknowledgeReview()
            assertFalse(vm.uiState.value.reviewRequired)
        }

    @Test fun processRestoreKeepsDirtyDraftAndPendingWrite() =
        runTest {
            val draft =
                GarageDraft
                    .new(
                        GarageForm.Fuel,
                        1,
                    ).set("quantity0", "2.123456789")
                    .set("odometer", "120")
            val vm =
                vm(
                    SavedStateHandle(
                        mapOf(
                            "draft" to draft.saved(),
                            "vehicle" to 1L,
                            "pending" to true,
                            "tab" to "Fuel",
                        ),
                    ),
                )
            runCurrent()
            assertEquals(draft, vm.uiState.value.draft)
            assertTrue(vm.uiState.value.reviewRequired)
            assertEquals(GarageTab.Fuel, vm.uiState.value.tab)
        }

    @Test fun validationsRejectNegativeValuesMissingOperandsAndBackwardNewOdometer() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.select(1)
            runCurrent()
            val base =
                GarageDraft
                    .new(
                        GarageForm.Fuel,
                        1,
                    ).set("odometer", "99")
                    .set("quantity0", "2")
                    .set("price0", "100")
            vm.edit(base)
            vm.save()
            runCurrent()
            assertTrue(repo.writes.isEmpty())
            assertEquals(
                uiText(R.string.pulse_garage_odometer_must_be_at_least_1_s_km_the, "100"),
                vm.uiState.value.error,
            )
            val apply = ApplyGarageChange(repo)
            assertFalse(
                apply.valid(GarageChange.Pressure(1, TirePressure(frontSolo = "-1".toBigDecimal()))),
            )
            assertFalse(
                apply.valid(
                    (base.change() as GarageChange.Fuel).copy(
                        items = listOf(FuelItem("petrol", "full", "1".toBigDecimal(), null, null)),
                    ),
                ),
            )
            assertFalse(
                apply.valid(
                    GarageChange.Care(1, null, "bad", "Care", "2".toBigDecimal(), Instant.EPOCH, null, null, null),
                ),
            )
            assertTrue(
                apply.valid((base.change() as GarageChange.Fuel).copy(odometer = "120".toBigDecimal())),
            )
        }

    @Test fun deletionRemovesMissingSelectionAndKeepsNoStaleDetail() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.select(1)
            runCurrent()
            repo.write =
                {
                    repo.vehicles = emptyList()
                    Outcome.Success(Unit)
                }
            vm.mutate(GarageChange.Delete(GarageRecord.Vehicle, 1))
            runCurrent()
            assertNull(vm.uiState.value.vehicle)
            assertNull(vm.uiState.value.history.value)
        }

    @Test fun failedAttachmentKeepsMaintenanceAndAllowsReview() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.select(1)
            runCurrent()
            repo.uploadResult =
                Outcome.Error(Failure.Server)
            vm.upload(1, 2, "content://fixture/receipt")
            runCurrent()
            assertTrue(vm.uiState.value.reviewRequired)
            assertNotNull(vm.uiState.value.history.value)
            assertNull(vm.uiState.value.progress)
            assertEquals(
                uiText(R.string.pulse_garage_uncertain_change, Failure.Server.message),
                vm.uiState.value.message,
            )
        }

    @Test fun editingPreservesTimestampAndDecimalAcrossDateBoundaries() {
        val instant = Instant.parse("2026-12-31T23:59:59.123456Z")
        val fuel =
            FuelFill(
                1,
                "123.123456789".toBigDecimal(),
                instant,
                "Station",
                null,
                listOf(
                    FuelItem(
                        "petrol",
                        "partial",
                        "1.23".toBigDecimal(),
                        "90.11".toBigDecimal(),
                        null,
                    ),
                ),
            )
        val change = fuel.draft(2).change() as GarageChange.Fuel
        assertEquals(instant, change.date)
        assertEquals(fuel.odometer, change.odometer)
        assertEquals(fuel.items, change.items)
        assertEquals(fuel.draft(2), GarageDraft.restore(fuel.draft(2).saved()))
    }

    @Test fun unchangedAmbiguousTimestampKeepsItsOriginalOffsetAndGapsAreRejected() {
        val originalZone = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"))
            val instant = Instant.parse("2026-11-01T06:30:00Z")
            val item = FuelItem("petrol", "full", "1".toBigDecimal(), "100".toBigDecimal(), null)
            val draft = FuelFill(1, "1".toBigDecimal(), instant, null, null, listOf(item)).draft(1)
            assertEquals(instant, (draft.change() as GarageChange.Fuel).date)
            val gap = draft.set("date", "2026-03-08").set("time", "02:30")
            try {
                gap.change()
                fail("Nonexistent local time accepted")
            } catch (
                _: IllegalArgumentException,
            ) {
            }
        } finally {
            java.util.TimeZone.setDefault(originalZone)
        }
    }

    @Test fun storageNumericLimitsRejectSilentRoundingAndOverflow() {
        val apply = ApplyGarageChange(repo)
        assertFalse(
            apply.valid(GarageChange.Pressure(1, TirePressure(frontSolo = "28.25".toBigDecimal()))),
        )
        assertFalse(
            apply.valid(GarageChange.Pressure(1, TirePressure(frontSolo = "10000".toBigDecimal()))),
        )
        assertFalse(apply.valid(GarageChange.CreateVehicle("v".repeat(49), true, TirePressure(), "two_wheeler", listOf("petrol"))))
        assertFalse(
            apply.valid(
                GarageChange.Care(1, null, "service", "Care", "1.001".toBigDecimal(), Instant.EPOCH, null, null, null),
            ),
        )
        assertTrue(
            apply.valid(
                GarageChange.Care(
                    1,
                    null,
                    "service",
                    "Care",
                    "9999999999.99".toBigDecimal(),
                    Instant.EPOCH,
                    "99999999999.9".toBigDecimal(),
                    null,
                    null,
                ),
            ),
        )
    }

    @Test fun everyMaintenanceCategoryAndTwoFuelTanksValidate() {
        val apply = ApplyGarageChange(repo)
        garageCategories.forEach {
            assertTrue(
                apply.valid(
                    GarageChange.Care(1, null, it, "Care", "1.23".toBigDecimal(), Instant.EPOCH, null, null, null),
                ),
            )
        }
        val item = FuelItem("diesel", "missed", "1".toBigDecimal(), null, "2".toBigDecimal())
        assertTrue(
            apply.valid(
                GarageChange.Fuel(
                    1,
                    1,
                    "0".toBigDecimal(),
                    Instant.EPOCH,
                    null,
                    null,
                    listOf(item, item.copy(type = "lpg")),
                ),
            ),
        )
        assertFalse(
            apply.valid(
                GarageChange.Fuel(
                    1,
                    1,
                    "0".toBigDecimal(),
                    Instant.EPOCH,
                    null,
                    null,
                    listOf(item, item, item),
                ),
            ),
        )
    }

    @Test fun vehicleDetailsAreRequiredAndDualTankDraftSurvivesRestore() =
        runTest {
            val vm = vm()
            runCurrent()
            val draft = GarageDraft.new(GarageForm.Vehicle, 0).set("name", "Tourer")
            vm.edit(draft)
            vm.save()
            runCurrent()
            assertTrue(repo.writes.isEmpty())
            assertNotNull(vm.uiState.value.error)

            val single = draft.set("vehicleType", "four_wheeler").set("vehicleFuel", "petrol")
            val apply = ApplyGarageChange(repo)
            assertTrue(apply.valid(single.change()))
            assertEquals(listOf("petrol"), (single.change() as GarageChange.CreateVehicle).fuelTypes)
            val dual = single.set("dualTank", "true")
            assertFalse(apply.valid(dual.change()))
            assertFalse(apply.valid(dual.set("vehicleSecondFuel", "petrol").change()))
            assertFalse(apply.valid(single.set("vehicleType", "truck").change()))
            assertFalse(apply.valid(single.set("vehicleFuel", "water").change()))
            val complete = dual.set("vehicleSecondFuel", "cng")
            val restored = GarageDraft.restore(complete.saved())
            assertEquals(complete, restored)
            val change = restored.change() as GarageChange.CreateVehicle
            assertTrue(apply.valid(change))
            assertEquals("four_wheeler", change.vehicleType)
            assertEquals(listOf("petrol", "cng"), change.fuelTypes)
            assertEquals(listOf("petrol"), (restored.set("dualTank", "false").change() as GarageChange.CreateVehicle).fuelTypes)
            vm.edit(restored)
            vm.save()
            runCurrent()
            assertEquals(listOf(change), repo.writes)
        }
}
