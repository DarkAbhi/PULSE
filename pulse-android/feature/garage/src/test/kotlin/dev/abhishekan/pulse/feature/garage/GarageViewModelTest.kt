package dev.abhishekan.pulse.feature.garage

import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.core.ui.message
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.repository.GarageRepository
import dev.abhishekan.pulse.core.domain.usecase.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.time.*

@OptIn(ExperimentalCoroutinesApi::class)
class GarageViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private class Repo : GarageRepository {
        override val changes = MutableStateFlow(0L)
        var vehicles = listOf(Vehicle(1, "Ride"))
        var failure: Failure? = null
        var reads = 0
        val writes = mutableListOf<GarageChange>()
        var write: suspend () -> Outcome<Unit> = { Outcome.Success(Unit) }
        var uploadResult: Outcome<Unit> = Outcome.Success(Unit)
        val history = VehicleHistory("Ride", TirePressure(), emptyList(), listOf(FuelFill(2, "100".toBigDecimal(), Instant.EPOCH, null, null,
            listOf(FuelItem("petrol", "full", "2".toBigDecimal(), "100".toBigDecimal(), "200".toBigDecimal())))), emptyList(), emptyMap())
        override suspend fun vehicles() = Outcome.Success(vehicles)
        override suspend fun history(vehicle: Long): Outcome<VehicleHistory> { reads++; return failure?.let { Outcome.Error(it) } ?: Outcome.Success(history) }
        override suspend fun apply(change: GarageChange): Outcome<Unit> { writes += change; return write() }
        override suspend fun upload(vehicle: Long, record: Long, source: String, progress: (Float) -> Unit): Outcome<Unit> { progress(.5f); return uploadResult }
        override suspend fun download(vehicle: Long, record: Long, receipt: GarageReceipt, destination: String?) = Outcome.Success("content://preview/receipt")
    }
    private val repo = Repo()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { models.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }
    private fun vm(saved: SavedStateHandle = SavedStateHandle()) = GarageViewModel(GetVehicles(repo), GetVehicleHistory(repo), ApplyGarageChange(repo),
        UploadGarageReceipt(repo), DownloadGarageReceipt(repo), saved).also { models.put("garage", it) }
    @Test fun historyFailureKeepsCollectionAndRetries() = runTest {
        val vm = vm(); runCurrent(); repo.failure = Failure.Connection; vm.select(1); runCurrent()
        assertEquals(1, vm.uiState.value.vehicles.value?.size); assertEquals(Failure.Connection, vm.uiState.value.history.error)
        repo.failure = null; vm.refreshHistory(); runCurrent(); assertNotNull(vm.uiState.value.history.value)
    }
    @Test fun duplicateWritesBlockedAndRefreshAfterSuccess() = runTest {
        val gate = CompletableDeferred<Outcome<Unit>>(); repo.write = { gate.await() }
        val vm = vm(); runCurrent(); vm.select(1); runCurrent()
        vm.mutate(GarageChange.Air(1)); vm.mutate(GarageChange.Air(1)); runCurrent(); assertEquals(1, repo.writes.size)
        gate.complete(Outcome.Success(Unit)); runCurrent(); assertEquals(2, repo.reads); assertFalse(vm.uiState.value.busy)
    }
    @Test fun failedSaveRequiresRefreshedRecordsAndExplicitReview() = runTest {
        repo.write = { repo.failure = Failure.Connection; Outcome.Error(Failure.Timeout) }
        val vm = vm(); runCurrent(); vm.select(1); runCurrent(); vm.mutate(GarageChange.Air(1)); runCurrent()
        assertTrue(vm.uiState.value.reviewRequired); vm.acknowledgeReview(); vm.mutate(GarageChange.Air(1)); runCurrent(); assertEquals(1, repo.writes.size)
        repo.failure = null; vm.refresh(); runCurrent(); vm.acknowledgeReview(); assertFalse(vm.uiState.value.reviewRequired)
    }
    @Test fun processRestoreKeepsDirtyDraftAndPendingWrite() = runTest {
        val draft = GarageDraft.new(GarageForm.Fuel, 1).set("quantity0", "2.123456789").set("odometer", "120")
        val vm = vm(SavedStateHandle(mapOf("draft" to draft.saved(), "vehicle" to 1L, "pending" to true, "tab" to "Fuel")))
        runCurrent(); assertEquals(draft, vm.uiState.value.draft); assertTrue(vm.uiState.value.reviewRequired); assertEquals(GarageTab.Fuel, vm.uiState.value.tab)
    }
    @Test fun validationsRejectNegativeValuesMissingOperandsAndBackwardNewOdometer() = runTest {
        val vm = vm(); runCurrent(); vm.select(1); runCurrent()
        val base = GarageDraft.new(GarageForm.Fuel, 1).set("odometer", "99").set("quantity0", "2").set("price0", "100")
        vm.edit(base); vm.save(); runCurrent(); assertTrue(repo.writes.isEmpty()); assertEquals(uiText(R.string.pulse_garage_odometer_must_be_at_least_1_s_km_the, "100"), vm.uiState.value.error)
        val apply = ApplyGarageChange(repo)
        assertFalse(apply.valid(GarageChange.Pressure(1, TirePressure(frontSolo = "-1".toBigDecimal()))))
        assertFalse(apply.valid((base.change() as GarageChange.Fuel).copy(items = listOf(FuelItem("petrol", "full", "1".toBigDecimal(), null, null)))))
        assertFalse(apply.valid(GarageChange.Care(1, null, "bad", "Care", "2".toBigDecimal(), Instant.EPOCH, null, null, null)))
        assertTrue(apply.valid((base.change() as GarageChange.Fuel).copy(odometer = "120".toBigDecimal())))
    }
    @Test fun deletionRemovesMissingSelectionAndKeepsNoStaleDetail() = runTest {
        val vm = vm(); runCurrent(); vm.select(1); runCurrent(); repo.write = { repo.vehicles = emptyList(); Outcome.Success(Unit) }
        vm.mutate(GarageChange.Delete(GarageRecord.Vehicle, 1)); runCurrent(); assertNull(vm.uiState.value.vehicle); assertNull(vm.uiState.value.history.value)
    }
    @Test fun failedAttachmentKeepsMaintenanceAndAllowsReview() = runTest {
        val vm = vm(); runCurrent(); vm.select(1); runCurrent(); repo.uploadResult = Outcome.Error(Failure.Server)
        vm.upload(1, 2, "content://fixture/receipt"); runCurrent()
        assertTrue(vm.uiState.value.reviewRequired); assertNotNull(vm.uiState.value.history.value); assertNull(vm.uiState.value.progress)
        assertEquals(uiText(R.string.pulse_garage_uncertain_change, Failure.Server.message), vm.uiState.value.message)
    }
    @Test fun editingPreservesTimestampAndDecimalAcrossDateBoundaries() {
        val instant = Instant.parse("2026-12-31T23:59:59.123456Z")
        val fuel = FuelFill(1, "123.123456789".toBigDecimal(), instant, "Station", null, listOf(FuelItem("petrol", "partial", "1.23".toBigDecimal(), "90.11".toBigDecimal(), null)))
        val change = fuel.draft(2).change() as GarageChange.Fuel
        assertEquals(instant, change.date); assertEquals(fuel.odometer, change.odometer); assertEquals(fuel.items, change.items)
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
            try { gap.change(); fail("Nonexistent local time accepted") } catch (_: IllegalArgumentException) { }
        } finally { java.util.TimeZone.setDefault(originalZone) }
    }
    @Test fun storageNumericLimitsRejectSilentRoundingAndOverflow() {
        val apply = ApplyGarageChange(repo)
        assertFalse(apply.valid(GarageChange.Pressure(1, TirePressure(frontSolo = "28.25".toBigDecimal()))))
        assertFalse(apply.valid(GarageChange.Pressure(1, TirePressure(frontSolo = "10000".toBigDecimal()))))
        assertFalse(apply.valid(GarageChange.CreateVehicle("v".repeat(49),true,TirePressure())))
        assertFalse(apply.valid(GarageChange.Care(1,null,"service","Care","1.001".toBigDecimal(),Instant.EPOCH,null,null,null)))
        assertTrue(apply.valid(GarageChange.Care(1,null,"service","Care","9999999999.99".toBigDecimal(),Instant.EPOCH,"99999999999.9".toBigDecimal(),null,null)))
    }
    @Test fun everyMaintenanceCategoryAndTwoFuelTanksValidate() {
        val apply = ApplyGarageChange(repo)
        garageCategories.forEach { assertTrue(apply.valid(GarageChange.Care(1, null, it, "Care", "1.23".toBigDecimal(), Instant.EPOCH, null, null, null))) }
        val item = FuelItem("diesel", "missed", "1".toBigDecimal(), null, "2".toBigDecimal())
        assertTrue(apply.valid(GarageChange.Fuel(1, 1, "0".toBigDecimal(), Instant.EPOCH, null, null, listOf(item, item.copy(type = "lpg")))))
        assertFalse(apply.valid(GarageChange.Fuel(1, 1, "0".toBigDecimal(), Instant.EPOCH, null, null, listOf(item, item, item))))
    }
}
