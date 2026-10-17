package dev.abhishekan.pulse.feature.garage

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun GarageRoute(onDraftChanged: (Boolean) -> Unit, viewModel: GarageViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pickerVehicle by rememberSaveable { mutableStateOf<Long?>(null) }
    var pickerRecord by rememberSaveable { mutableStateOf<Long?>(null) }
    var downloadVehicle by rememberSaveable { mutableStateOf<Long?>(null) }
    var downloadRecord by rememberSaveable { mutableStateOf<Long?>(null) }
    var downloadReceipt by rememberSaveable { mutableStateOf<List<String>?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val v = pickerVehicle; val record = pickerRecord
        if (uri != null && v != null && record != null) viewModel.upload(v, record, uri.toString())
        pickerVehicle = null; pickerRecord = null
    }
    val download = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val r = downloadReceipt; val record = downloadRecord
        if (uri != null && r != null && record != null) viewModel.download(record, GarageReceipt(r[0].toLong(), r[1], r[2], r[3].toLong()), uri.toString(), downloadVehicle)
        downloadVehicle = null; downloadRecord = null; downloadReceipt = null
    }
    LaunchedEffect(state.draft?.dirty) { onDraftChanged(state.draft?.dirty == true) }
    LaunchedEffect(state.openUri) {
        state.openUri?.let { uri ->
            try {
                val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri.toUri(), state.openType)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(intent, "Open receipt"))
            } catch (_: ActivityNotFoundException) { viewModel.fileMessage("No app can open this receipt. Use Download to save a copy.") }
            catch (_: SecurityException) { viewModel.fileMessage("The receipt could not be opened. Download a copy instead.") }
            viewModel.opened()
        }
    }
    GarageScreen(state, viewModel, { record ->
        pickerVehicle = state.vehicle; pickerRecord = record
        try { pick.launch(arrayOf("*/*")) } catch (_: ActivityNotFoundException) { viewModel.fileMessage("A system file picker is unavailable on this device.") }
    }, { record, receipt ->
        downloadVehicle = state.vehicle; downloadRecord = record; downloadReceipt = arrayListOf(receipt.id.toString(), receipt.name, receipt.contentType, receipt.size.toString())
        try { download.launch(receipt.name) } catch (_: ActivityNotFoundException) { viewModel.fileMessage("A system document picker is unavailable on this device.") }
    })
}

@Composable
internal fun GarageScreen(state: GarageUiState, vm: GarageViewModel, upload: (Long) -> Unit, download: (Long, GarageReceipt) -> Unit) {
    var discard by rememberSaveable { mutableStateOf(false) }
    var records by rememberSaveable { mutableStateOf(false) }
    var deletion by remember { mutableStateOf<Pair<GarageChange.Delete, String>?>(null) }
    val cancel = { if (state.draft?.dirty == true) discard = true else vm.discard() }
    BackHandler(state.draft != null || state.vehicle != null || state.busy) {
        when { state.busy -> Unit; state.draft != null -> cancel(); else -> vm.select(null) }
    }
    val scrollStates = rememberSaveableStateHolder()
    val enabled = !state.busy && !state.reviewRequired && state.draft == null
    PulseAdaptiveLayout(Modifier.fillMaxSize()) { size ->
        val twoColumns = size != PulseWindowSize.Compact && LocalDensity.current.fontScale <= 1.3f
        val content: @Composable () -> Unit = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
                .padding(if (size == PulseWindowSize.Compact) PulseTokens.medium else PulseTokens.large), verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                if (state.reviewRequired) PulseCard(Modifier.fillMaxWidth()) {
                    PulseText("Review before another change", style = PulseTextStyle.Title)
                    PulseText(state.message ?: "A previous save may have reached the server. Review current records before another change.")
                    PulseTextButton("Refresh server records", vm::refresh, enabled = !state.busy)
                    if (state.draft != null) PulseTextButton(if (records) "Return to draft" else "View server records", { records = !records })
                    PulseButton("I reviewed the server records", vm::acknowledgeReview, enabled = state.reviewReady && !state.busy)
                } else state.message?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (state.draft != null && !records) {
                    GarageEditor(state.draft, state.busy, state.reviewRequired, state.error, vm::update, vm::save, cancel)
                } else {
                    if (state.draft != null) PulseTextButton("Resume unsaved edits", { records = false })
                    val collection: @Composable () -> Unit = {
                        Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                            PulseText("Your garage", Modifier.semantics { heading() }, PulseTextStyle.Display)
                            PulseText("Keep every journey running smoothly.")
                            PulseButton("Add vehicle", { records = false; vm.create(GarageForm.Vehicle) }, enabled = enabled)
                            GarageSection(state.vehicles, vm::refreshVehicles) { vehicles ->
                                if (vehicles.isEmpty()) PulseEmptyState("Room for your first ride", "Add a vehicle to record fuel, tire pressure and maintenance.")
                                vehicles.forEach { vehicle -> PulseCard(Modifier.fillMaxWidth(), highlighted = vehicle.id == state.vehicle) {
                                    PulseText(vehicle.name, style = PulseTextStyle.Title)
                                    PulseTextButton("View ${vehicle.name}", { vm.select(vehicle.id) }, enabled = !state.busy && state.draft == null)
                                } }
                            }
                        }
                    }
                    val details: @Composable () -> Unit = {
                        Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                            if (state.vehicle != null) {
                                if (!twoColumns) PulseTextButton("All vehicles", { vm.select(null) }, enabled = !state.busy && state.draft == null)
                                GarageSection(state.history, vm::refreshHistory) { data ->
                                    val vehicle = checkNotNull(state.vehicle)
                                    PulseText(data.name, Modifier.semantics { heading() }, PulseTextStyle.Display)
                                    val remove: (GarageChange.Delete, String) -> Unit = { change, body -> deletion = change to body }
                                    val edit: (GarageDraft) -> Unit = { records = false; vm.edit(it) }
                                    when(state.tab) {
                                        GarageTab.Overview -> {
                                            EfficiencyCards(data)
                                            PulseCard(Modifier.fillMaxWidth()) {
                                                PulseText("Care, at a glance", style = PulseTextStyle.Title)
                                                PulseText("${data.fuel.size} fuel fill-ups · ${data.maintenance.size} maintenance records · ${data.air.size} air fills")
                                                data.maintenance.firstOrNull()?.let { PulseText("Latest care: ${it.title} · ${garageDate(it.date)}") }
                                                PulseButton("Add fuel fill-up", { records = false; vm.create(GarageForm.Fuel) }, enabled = enabled)
                                                PulseTextButton("Add maintenance", { records = false; vm.create(GarageForm.Care) }, enabled = enabled)
                                            }
                                            PressureCard(data.pressure, enabled, vm::pressure, { vm.mutate(GarageChange.Air(vehicle)) })
                                            PulseTextButton("Delete ${data.name}", { remove(GarageChange.Delete(GarageRecord.Vehicle, vehicle),
                                                "Delete “${data.name}”? This permanently deletes the vehicle, fuel items, air fills, maintenance and receipt metadata. The current backend does not remove hosted receipt files during vehicle deletion; those files may remain in server storage.") }, enabled = enabled)
                                        }
                                        GarageTab.Fuel -> {
                                            PulseButton("Add fuel fill-up", { records = false; vm.create(GarageForm.Fuel) }, enabled = enabled)
                                            FuelCards(data, vehicle, enabled, edit, remove)
                                        }
                                        GarageTab.Care -> {
                                            PulseButton("Add maintenance", { records = false; vm.create(GarageForm.Care) }, enabled = enabled)
                                            MaintenanceCards(data, vehicle, enabled, state.busy, edit, remove, upload, { record, receipt -> vm.download(record, receipt) }, download, state.uploadRecord, state.progress)
                                        }
                                        GarageTab.Air -> {
                                            PressureCard(data.pressure, enabled, vm::pressure, { vm.mutate(GarageChange.Air(vehicle)) })
                                            if (data.air.isEmpty()) PulseEmptyState("No air fills recorded", "Record an air fill after checking your tires.")
                                            data.air.forEach { air -> PulseCard(Modifier.fillMaxWidth()) {
                                                PulseText(garageDate(air.date), style = PulseTextStyle.Title)
                                                PulseTextButton("Delete air fill ${air.id}", { remove(GarageChange.Delete(GarageRecord.Air, vehicle, air.id), "Permanently delete this air-fill entry? Target tire pressures stay unchanged.") }, enabled = enabled)
                                            } }
                                        }
                                    }
                                    PulseTextButton("Refresh vehicle history", vm::refreshHistory, enabled = !state.busy)
                                }
                            }
                        }
                    }
                    if (twoColumns && state.vehicle != null) Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                        Box(Modifier.weight(1f)) { collection() }; Box(Modifier.weight(1.8f)) { details() }
                    } else if (state.vehicle == null) collection() else details()
                }
                if (state.busy && state.progress == null) PulseLoadingState("Working on your garage")
            }
        }
        Column(Modifier.widthIn(max = PulseTokens.contentWidth).fillMaxSize()) {
            if (state.vehicle != null && (state.draft == null || records)) Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = PulseTokens.medium),
                horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                GarageTab.entries.forEach { tab -> PulseFilterChip(tab.label, state.tab == tab, { vm.tab(tab) }, enabled = !state.busy) }
            }
            Box(Modifier.weight(1f)) {
                if (state.draft != null && !records) key(state.draft.kind, state.draft.id) { content() }
                else scrollStates.SaveableStateProvider("${state.vehicle}-${state.tab}") { content() }
            }
        }
    }
    if (discard) PulseDialog("Discard unsaved changes?", "Your edits have not been saved. Discard this device’s draft?", "Discard edits", { discard = false; records = false; vm.discard() }, { discard = false })
    deletion?.let { (change, body) -> PulseDialog("Confirm deletion", body, "Delete", { deletion = null; vm.mutate(change) }, { deletion = null }) }
}
@Composable
private fun <T> GarageSection(section: GarageSection<T>, retry: () -> Unit, content: @Composable (T) -> Unit) {
    when { section.loading -> PulseLoadingState("Loading your garage"); section.error != null -> PulseErrorState(section.error.message, retry); section.value != null -> content(section.value) }
}
