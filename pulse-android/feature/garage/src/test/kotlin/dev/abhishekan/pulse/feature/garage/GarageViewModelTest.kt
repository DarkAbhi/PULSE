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
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.domain.model.Vehicle
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogMatch
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogOption
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.core.domain.model.garageCategories
import dev.abhishekan.pulse.core.domain.repository.GarageRepository
import dev.abhishekan.pulse.core.domain.repository.VehicleCatalogRepository
import dev.abhishekan.pulse.core.domain.usecase.ApplyGarageChange
import dev.abhishekan.pulse.core.domain.usecase.DownloadGarageReceipt
import dev.abhishekan.pulse.core.domain.usecase.GetVehicleHistory
import dev.abhishekan.pulse.core.domain.usecase.SearchVehicleCatalog
import dev.abhishekan.pulse.core.domain.usecase.UploadGarageReceipt
import dev.abhishekan.pulse.core.ui.UnsavedChanges
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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
        var vehicles = listOf(Vehicle(1, "Ride", "motorcycle", listOf("petrol")))
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
                "motorcycle",
                listOf("petrol"),
            )

        override suspend fun vehicles() = Outcome.Success(vehicles)

        override suspend fun history(vehicle: Long): Outcome<VehicleHistory> {
            reads++
            val error = failure ?: Failure.NotFound.takeIf { vehicles.none { it.id == vehicle } }
            return error?.let { Outcome.Error(it) } ?: Outcome.Success(history)
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

    private val catalogCalls = mutableListOf<Triple<String, String?, String>>()
    private var catalogFailure: Failure? = null
    private val matchCalls = mutableListOf<Triple<String, String, String>>()
    private val catalog =
        object : VehicleCatalogRepository {
            override suspend fun match(
                kind: String,
                makeName: String,
                modelName: String,
            ): Outcome<VehicleCatalogMatch?> {
                matchCalls += Triple(kind, makeName, modelName)
                return catalogFailure?.let { Outcome.Error(it) } ?: Outcome.Success(
                    if (makeName == "Honda" &&
                        modelName == "Civic"
                    ) {
                        VehicleCatalogMatch("car/honda/civic", "honda", "Honda", "Civic")
                    } else {
                        null
                    },
                )
            }

            override suspend fun search(
                kind: String,
                makeSlug: String?,
                query: String,
            ): Outcome<List<VehicleCatalogOption>> {
                catalogCalls += Triple(kind, makeSlug, query)
                return catalogFailure?.let { Outcome.Error(it) } ?: Outcome.Success(
                    listOf(
                        if (makeSlug ==
                            null
                        ) {
                            VehicleCatalogOption("honda", "Honda")
                        } else {
                            VehicleCatalogOption("$kind/honda/civic", "Civic")
                        },
                    ),
                )
            }
        }

    @Test fun manualMatchDebouncesDoesNotSelectAndClearsOnInputChange() =
        runTest {
            val vm = vm()
            vm.addVehicle()
            vm.update(
                vm.editorState.value.draft!!
                    .set(
                        "catalogMode",
                        "manual",
                    ).set("vehicleKind", "car")
                    .set("makeName", "Honda")
                    .set("modelName", "Civi"),
            )
            advanceTimeBy(200)
            vm.update(
                vm.editorState.value.draft!!
                    .set("modelName", "Civic"),
            )
            advanceUntilIdle()
            assertEquals(listOf(Triple("car", "Honda", "Civic")), matchCalls)
            val draft = vm.editorState.value.draft!!
            assertEquals("manual", draft["catalogMode"])
            assertEquals("", draft["catalogId"])
            assertEquals("Honda Civic", draft["name"])
            assertTrue(repo.writes.isEmpty())
            val match = vm.editorState.value.match.value!!
            val accepted = draft.set("name", "My car").useMatch(match)
            assertEquals("car/honda/civic", accepted["catalogId"])
            assertEquals("My car", accepted["name"])
            vm.update(draft.set("modelName", "Other"))
            assertNull(vm.editorState.value.match.value)
            advanceUntilIdle()
            assertNull(vm.editorState.value.match.value)
        }

    @Test fun vehicleWizardRestoresStepAndValidatesRequiredChoices() {
        val empty = GarageDraft.new(GarageForm.Vehicle, 0)
        assertEquals("", empty["vehicleKind"])
        assertEquals("", empty["vehicleFuel"])
        assertFalse(empty.vehicleIdentityValid)
        val named = empty.set("catalogMode", "manual").set("vehicleKind", "car").set("name", "Comet")
        assertTrue(named.vehicleIdentityValid)
        assertFalse(named.vehicleFuelValid)
        val fuel = named.set("vehicleFuel", "petrol").set("dualTank", "true")
        assertFalse(fuel.vehicleFuelValid)
        assertFalse(fuel.set("vehicleSecondFuel", "petrol").vehicleFuelValid)
        val ready = fuel.set("vehicleSecondFuel", "cng").set("vehicleStep", "2")
        assertTrue(ready.vehicleFuelValid)
        assertEquals(2, GarageDraft.restore(ready.saved()).vehicleStep)
        assertEquals("", ready["frontSolo"])
        assertEquals("1", ready.adjustPressure("frontSolo", 1)["frontSolo"])
        assertEquals("0", ready.set("frontSolo", "0.5").adjustPressure("frontSolo", -1)["frontSolo"])
        assertEquals("9999.9", ready.set("frontSolo", "9999.5").adjustPressure("frontSolo", 1)["frontSolo"])
        assertFalse(ready.set("frontSolo", "28.25").validPressure("frontSolo"))
        assertEquals("28.25", ready.set("frontSolo", "28.25").adjustPressure("frontSolo", 1)["frontSolo"])
    }

    @Test fun catalogSearchDebouncesAndKindChangeClearsDependentIdentity() =
        runTest {
            val vm = vm()
            vm.addVehicle()
            vm.update(
                vm.editorState.value.draft!!
                    .set("vehicleKind", "car")
                    .set("makeQuery", "H"),
            )
            vm.update(
                vm.editorState.value.draft!!
                    .set("makeQuery", "Honda"),
            )
            advanceTimeBy(251)
            runCurrent()
            assertEquals(listOf(Triple("car", null, "Honda")), catalogCalls)
            vm.update(
                vm.editorState.value.draft!!
                    .chooseMake(VehicleCatalogOption("honda", "Honda")),
            )
            advanceUntilIdle()
            assertEquals(
                "car/honda/civic",
                vm.editorState.value.models.value!!
                    .single()
                    .id,
            )
            vm.update(
                vm.editorState.value.draft!!
                    .chooseModel(VehicleCatalogOption("car/honda/civic", "Civic")),
            )
            assertEquals("Honda Civic", vm.editorState.value.draft!!["name"])
            vm.update(
                vm.editorState.value.draft!!
                    .set("vehicleKind", "motorcycle"),
            )
            assertEquals("", vm.editorState.value.draft!!["catalogId"])
            assertEquals("", vm.editorState.value.draft!!["makeSlug"])
            assertNull(vm.editorState.value.models.value)
            advanceUntilIdle()
            assertEquals("motorcycle", catalogCalls.last().first)
        }

    @Test fun catalogFailureCanRetryOrSwitchToManualWithoutLosingNickname() =
        runTest {
            catalogFailure = Failure.Connection
            val vm = vm()
            vm.addVehicle()
            vm.update(
                vm.editorState.value.draft!!
                    .set("vehicleKind", "bus")
                    .set("name", "Our bus"),
            )
            advanceUntilIdle()
            assertEquals(Failure.Connection, vm.editorState.value.makes.error)
            catalogFailure = null
            vm.retryCatalog()
            advanceUntilIdle()
            assertEquals(
                "Honda",
                vm.editorState.value.makes.value!!
                    .single()
                    .name,
            )
            vm.update(
                vm.editorState.value.draft!!
                    .set("catalogMode", "manual")
                    .set("vehicleFuel", "diesel"),
            )
            vm.save()
            advanceUntilIdle()
            val change = repo.writes.single() as GarageChange.SaveVehicle
            assertEquals("Our bus", change.name)
            assertEquals("bus", change.vehicleKind)
            assertNull(change.catalogId)
        }

    @Test fun selectedCatalogModelAndSearchSurviveSavedDraftRestore() =
        runTest {
            val selected =
                GarageDraft
                    .new(GarageForm.Vehicle, 0)
                    .set("vehicleKind", "car")
                    .chooseMake(VehicleCatalogOption("honda", "Honda"))
                    .chooseModel(VehicleCatalogOption("car/honda/civic", "Civic"))
                    .set("name", "Weekend car")
                    .set("modelQuery", "Civic")
                    .set("vehicleFuel", "petrol")
            val vm = vm(SavedStateHandle(mapOf("draft" to selected.saved())))
            advanceUntilIdle()
            assertEquals("Civic", vm.editorState.value.draft!!["modelQuery"])
            vm.save()
            advanceUntilIdle()
            val change = repo.writes.single() as GarageChange.SaveVehicle
            assertEquals("car/honda/civic", change.catalogId)
            assertEquals("Weekend car", change.name)
            assertEquals("Honda", change.makeName)
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
            repo,
            ApplyGarageChange(repo),
            saved,
            SearchVehicleCatalog(catalog),
            UnsavedChanges(),
        ).also { models.put("garage", it) }

    private fun detail(
        id: Long = 1,
        saved: SavedStateHandle = SavedStateHandle(mapOf("id" to id)),
    ) = VehicleDetailViewModel(
        GetVehicleHistory(repo),
        ApplyGarageChange(repo),
        UploadGarageReceipt(repo),
        DownloadGarageReceipt(repo),
        saved,
        SearchVehicleCatalog(catalog),
        UnsavedChanges(),
    ).also { models.put("vehicle/$id", it) }

    @Test fun refreshingDetailKeepsHistoryOnFailure() =
        runTest {
            val vm = detail()
            runCurrent()
            val previous = vm.uiState.value.history.value
            repo.failure = Failure.Connection
            vm.refresh()
            assertTrue(vm.uiState.value.history.loading)
            assertEquals(previous, vm.uiState.value.history.value)
            runCurrent()
            assertEquals(Failure.Connection, vm.uiState.value.history.error)
            assertEquals(previous, vm.uiState.value.history.value)

            repo.failure = null
            vm.refresh()
            runCurrent()
            assertNotNull(vm.uiState.value.history.value)
            assertNull(vm.uiState.value.history.error)
        }

    @Test fun eachVehicleDetailEntryStartsOnOverview() =
        runTest {
            repo.vehicles = repo.vehicles + Vehicle(2, "Other", "motorcycle", listOf("petrol"))
            val first = detail()
            runCurrent()
            first.tab(GarageTab.Care)

            val other = detail(2)
            assertEquals(2L, other.uiState.value.vehicleId)
            assertEquals(GarageTab.Overview, other.uiState.value.tab)
            assertEquals(GarageTab.Care, first.uiState.value.tab)

            val reopened = detail()
            assertEquals(GarageTab.Overview, reopened.uiState.value.tab)
        }

    @Test fun missingVehicleClearsDetailAndReportsNotFound() =
        runTest {
            val vm = detail(999)
            runCurrent()
            assertNull(vm.uiState.value.history.value)
            assertEquals(Failure.NotFound, vm.uiState.value.history.error)
        }

    @Test fun duplicateWritesBlockedAndRefreshAfterSuccess() =
        runTest {
            val gate = CompletableDeferred<Outcome<Unit>>()
            repo.write = { gate.await() }
            val vm = detail()
            runCurrent()
            vm.mutate(GarageChange.Air(1))
            vm.mutate(GarageChange.Air(1))
            runCurrent()
            assertEquals(1, repo.writes.size)
            gate.complete(Outcome.Success(Unit))
            runCurrent()
            assertEquals(2, repo.reads)
            assertFalse(vm.editorState.value.busy)
        }

    @Test fun failedSaveRequiresRefreshedRecordsAndExplicitReview() =
        runTest {
            repo.write = {
                repo.failure = Failure.Connection
                Outcome.Error(Failure.Timeout)
            }
            val vm = detail()
            runCurrent()
            vm.mutate(GarageChange.Air(1))
            runCurrent()
            assertTrue(vm.editorState.value.reviewRequired)
            vm.acknowledgeReview()
            vm.mutate(GarageChange.Air(1))
            runCurrent()
            assertEquals(1, repo.writes.size)
            repo.failure = null
            vm.refresh()
            runCurrent()
            vm.acknowledgeReview()
            assertFalse(vm.editorState.value.reviewRequired)
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
                detail(
                    saved =
                        SavedStateHandle(
                            mapOf(
                                "draft" to draft.saved(),
                                "id" to 1L,
                                "pending" to true,
                                "tab" to "Fuel",
                            ),
                        ),
                )
            runCurrent()
            assertEquals(draft, vm.editorState.value.draft)
            assertTrue(vm.editorState.value.reviewRequired)
            assertEquals(GarageTab.Fuel, vm.uiState.value.tab)
        }

    @Test fun validationsRejectNegativeValuesMissingOperandsAndBackwardNewOdometer() =
        runTest {
            val vm = detail()
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
                vm.editorState.value.error,
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

    @Test fun deletingVehicleClosesDetailAndRefreshesCollectionOnResume() =
        runTest {
            val collection = vm()
            collection.resume()
            val vm = detail()
            runCurrent()
            repo.write =
                {
                    repo.vehicles = emptyList()
                    Outcome.Success(Unit)
                }
            vm.mutate(GarageChange.Delete(GarageRecord.Vehicle, 1))
            runCurrent()
            assertTrue(vm.uiState.value.deleted)
            assertNull(vm.uiState.value.history.value)
            val previousVehicles = collection.vehicles.value.value
            assertEquals(1, previousVehicles?.size)
            collection.resume()
            runCurrent()
            assertEquals(emptyList<Vehicle>(), collection.vehicles.value.value)
        }

    @Test fun failedAttachmentKeepsMaintenanceAndAllowsReview() =
        runTest {
            val vm = detail()
            runCurrent()
            repo.uploadResult =
                Outcome.Error(Failure.Server)
            vm.upload(2, "content://fixture/receipt")
            runCurrent()
            assertTrue(vm.editorState.value.reviewRequired)
            assertNotNull(vm.uiState.value.history.value)
            assertNull(vm.uiState.value.progress)
            assertEquals(
                uiText(R.string.pulse_garage_uncertain_change, Failure.Server.message),
                vm.editorState.value.message,
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
        val originalZone = UserTimeZone.zone.id
        try {
            UserTimeZone
                .select("America/New_York")
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
            UserTimeZone
                .select(originalZone)
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
        assertFalse(apply.valid(GarageChange.SaveVehicle("v".repeat(49), true, TirePressure(), "motorcycle", listOf("petrol"))))
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
            val draft = GarageDraft.new(GarageForm.Vehicle, 0).set("catalogMode", "manual").set("name", "Tourer")
            vm.edit(draft)
            vm.save()
            runCurrent()
            assertTrue(repo.writes.isEmpty())
            assertNotNull(vm.editorState.value.error)

            val single = draft.set("vehicleKind", "car").set("vehicleFuel", "petrol")
            val apply = ApplyGarageChange(repo)
            assertTrue(apply.valid(single.change()))
            assertEquals(listOf("petrol"), (single.change() as GarageChange.SaveVehicle).fuelTypes)
            val dual = single.set("dualTank", "true")
            assertFalse(apply.valid(dual.change()))
            assertFalse(apply.valid(dual.set("vehicleSecondFuel", "petrol").change()))
            assertFalse(apply.valid(single.set("vehicleKind", "spaceship").change()))
            assertFalse(apply.valid(single.set("vehicleFuel", "water").change()))
            val complete = dual.set("vehicleSecondFuel", "cng")
            val restored = GarageDraft.restore(complete.saved())
            assertEquals(complete, restored)
            val change = restored.change() as GarageChange.SaveVehicle
            assertTrue(apply.valid(change))
            assertEquals("car", change.vehicleKind)
            assertEquals(listOf("petrol", "cng"), change.fuelTypes)
            assertEquals(listOf("petrol"), (restored.set("dualTank", "false").change() as GarageChange.SaveVehicle).fuelTypes)
            vm.edit(restored)
            vm.save()
            runCurrent()
            assertEquals(listOf(change), repo.writes)
        }
}
