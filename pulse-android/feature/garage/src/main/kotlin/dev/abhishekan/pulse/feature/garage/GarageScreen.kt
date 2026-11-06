package dev.abhishekan.pulse.feature.garage

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.domain.model.GarageRecord
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseHideNavigation
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseRefreshIndicator
import dev.abhishekan.pulse.core.ui.components.PulseScreenScaffold
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import dev.abhishekan.pulse.core.ui.components.PulseTabs
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.rememberPullRefreshTracker
import dev.abhishekan.pulse.core.ui.uiText
import org.koin.androidx.compose.koinViewModel

@Composable
fun GarageRoute(
    viewModel: GarageViewModel = koinViewModel(),
    initialVehicle: Long? = null,
    backLabel: String? = null,
) {
    val currency by UserCurrency.changes
        .collectAsStateWithLifecycle()
    val timeZone by UserTimeZone.changes.collectAsStateWithLifecycle()
    LaunchedEffect(timeZone) { viewModel.refresh() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PulseHideNavigation(state.vehicle != null || state.draft != null)
    PulseSnackbarEffect(state.message.takeUnless { state.reviewRequired }, viewModel::consumeMessage)
    var initialVehicleOpened by rememberSaveable(initialVehicle) { mutableStateOf(false) }
    LaunchedEffect(initialVehicle) {
        if (!initialVehicleOpened) {
            initialVehicleOpened = true
            initialVehicle?.let(viewModel::select)
        }
    }
    val context = LocalContext.current
    val openReceiptTitle = stringResource(R.string.pulse_garage_open_receipt)
    var pickerVehicle by rememberSaveable { mutableStateOf<Long?>(null) }
    var pickerRecord by rememberSaveable { mutableStateOf<Long?>(null) }
    var downloadVehicle by rememberSaveable { mutableStateOf<Long?>(null) }
    var downloadRecord by rememberSaveable { mutableStateOf<Long?>(null) }
    var downloadReceipt by rememberSaveable { mutableStateOf<List<String>?>(null) }
    val pick =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val v = pickerVehicle
            val record = pickerRecord
            if (uri != null && v != null && record != null) viewModel.upload(v, record, uri.toString())
            pickerVehicle = null
            pickerRecord = null
        }
    val download =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream"),
        ) { uri ->
            val r = downloadReceipt
            val record = downloadRecord
            if (uri != null &&
                r != null &&
                record != null
            ) {
                viewModel.download(
                    record,
                    GarageReceipt(r[0].toLong(), r[1], r[2], r[3].toLong()),
                    uri.toString(),
                    downloadVehicle,
                )
            }
            downloadVehicle = null
            downloadRecord = null
            downloadReceipt = null
        }
    val vehicleTitle =
        if (state.vehicle != null) {
            state.history.value?.name ?: state.vehicles.value
                ?.firstOrNull { it.id == state.vehicle }
                ?.name
        } else {
            null
        }
    LaunchedEffect(state.openUri) {
        state.openUri?.let { uri ->
            try {
                val intent =
                    Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri.toUri(), state.openType)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(intent, openReceiptTitle))
            } catch (
                _: ActivityNotFoundException,
            ) {
                viewModel.fileMessage(
                    uiText(R.string.pulse_garage_no_app_can_open_this_receipt_use_download_to),
                )
            } catch (
                _: SecurityException,
            ) {
                viewModel.fileMessage(
                    uiText(R.string.pulse_garage_the_receipt_could_not_be_opened_download_a_copy),
                )
            }
            viewModel.opened()
        }
    }
    PulseScreenScaffold(
        title =
            state.draft?.let {
                stringResource(
                    it.kind.editorLabel(
                        if (it.id == null &&
                            it.kind != GarageForm.Pressure
                        ) {
                            EditorAction.Add
                        } else {
                            EditorAction.Edit
                        },
                    ),
                )
            } ?: state.reminderPage?.let { reminderPageTitle(it) } ?: vehicleTitle ?: stringResource(R.string.pulse_garage_screen_title),
        showTopBar = state.draft != null || state.vehicle != null,
        backLabel =
            if (state.draft != null) {
                stringResource(R.string.pulse_garage_back_to_garage)
            } else if (state.reminderPage != null) {
                stringResource(
                    if (state.reminderPage ==
                        "list"
                    ) {
                        R.string.pulse_garage_reminder_back_maintenance
                    } else {
                        R.string.pulse_garage_reminder_back_settings
                    },
                )
            } else {
                backLabel
                    ?: stringResource(R.string.pulse_garage_back_to_collection)
            },
        backEnabled = !state.busy,
    ) {
        key(timeZone, currency) {
            GarageScreen(state, viewModel, { record ->
                pickerVehicle = state.vehicle
                pickerRecord = record
                try {
                    pick.launch(arrayOf("*/*"))
                } catch (
                    _: ActivityNotFoundException,
                ) {
                    viewModel.fileMessage(
                        uiText(R.string.pulse_garage_a_system_file_picker_is_unavailable_on_this_device),
                    )
                }
            }, { record, receipt ->
                downloadVehicle = state.vehicle
                downloadRecord = record
                downloadReceipt =
                    arrayListOf(
                        receipt.id.toString(),
                        receipt.name,
                        receipt.contentType,
                        receipt.size.toString(),
                    )
                try {
                    download.launch(receipt.name)
                } catch (
                    _: ActivityNotFoundException,
                ) {
                    viewModel.fileMessage(
                        uiText(R.string.pulse_garage_a_system_document_picker_is_unavailable_on_this_device),
                    )
                }
            })
        }
    }
}

@Composable
internal fun GarageScreen(
    state: GarageUiState,
    vm: GarageViewModel,
    upload: (Long) -> Unit,
    download: (Long, GarageReceipt) -> Unit,
) {
    var discard by rememberSaveable { mutableStateOf(false) }
    var records by rememberSaveable { mutableStateOf(false) }
    var deletion by remember { mutableStateOf<Pair<GarageChange.Delete, UiText>?>(null) }
    val cancel = { if (state.draft?.dirty == true) discard = true else vm.discard() }
    BackHandler(state.draft != null || state.vehicle != null || state.busy) {
        when {
            state.busy -> Unit
            state.draft != null -> cancel()
            state.reminderPage != null -> vm.backFromReminder()
            else -> vm.select(null)
        }
    }
    val scrollStates = rememberSaveableStateHolder()
    val enabled = !state.busy && !state.reviewRequired && state.draft == null
    val refreshState = rememberPullToRefreshState()
    val tracker = rememberPullRefreshTracker(state.history.loading)
    val canRefresh = state.reminderPage == null && state.vehicle != null && !state.history.loading && !state.busy && state.draft == null
    val showLoading =
        !state.reviewRequired &&
            (state.draft == null || records) &&
            if (state.vehicle == null) state.vehicles.loading else state.history.loading
    val refresh = { if (canRefresh) tracker.request(vm::refreshHistory) }
    val refreshLabel = stringResource(R.string.pulse_garage_refresh_vehicle_history)
    PulseAdaptiveLayout(Modifier.fillMaxSize()) { size ->
        val content: @Composable () -> Unit = {
            val fuelEditor = state.draft?.kind == GarageForm.Fuel && !records
            val editorScroll = rememberScrollState()
            LaunchedEffect(state.draft?.kind, state.draft?.vehicleStep) {
                if (state.draft?.kind == GarageForm.Vehicle) editorScroll.scrollTo(0)
            }
            Column(
                Modifier.fillMaxSize().padding(LocalPulseContentPadding.current).imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(editorScroll)
                        .padding(size.pagePadding),
                    verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
                    horizontalAlignment = if (fuelEditor) Alignment.CenterHorizontally else Alignment.Start,
                ) {
                    if (state.reviewRequired) {
                        PulseCard(Modifier.fillMaxWidth()) {
                            PulseText(
                                stringResource(R.string.pulse_garage_review_before_another_change),
                                style = PulseTextStyle.Title,
                            )
                            PulseText(
                                state.message?.asString()
                                    ?: stringResource(
                                        R.string.pulse_garage_a_previous_save_may_have_reached_the_server_review,
                                    ),
                            )
                            PulseButton(
                                stringResource(R.string.pulse_garage_refresh_server_records),
                                vm::refresh,
                                enabled = !state.busy,
                                style = PulseButtonStyle.Text,
                            )
                            if (state.draft !=
                                null
                            ) {
                                PulseButton(
                                    if (records) {
                                        stringResource(
                                            R.string.pulse_garage_return_to_draft,
                                        )
                                    } else {
                                        stringResource(R.string.pulse_garage_view_server_records)
                                    },
                                    {
                                        records =
                                            !records
                                    },
                                    style = PulseButtonStyle.Text,
                                )
                            }
                            PulseButton(
                                stringResource(R.string.pulse_garage_i_reviewed_the_server_records),
                                vm::acknowledgeReview,
                                enabled =
                                    state.reviewReady && !state.busy,
                            )
                        }
                    }
                    if (state.draft != null && !records) {
                        GarageEditor(
                            state.draft,
                            state.busy,
                            state.reviewRequired,
                            state.error,
                            vm::update,
                            vm::save,
                            cancel,
                            state.makes,
                            state.models,
                            vm::retryCatalog,
                            state.match,
                        )
                    } else {
                        if (state.draft !=
                            null
                        ) {
                            PulseButton(
                                stringResource(
                                    R.string.pulse_garage_resume_unsaved_edits,
                                ),
                                {
                                    records =
                                        false
                                },
                                style = PulseButtonStyle.Text,
                            )
                        }
                        val collection: @Composable () -> Unit = {
                            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                                GarageCollectionHeader(enabled) {
                                    records = false
                                    vm.create(GarageForm.Vehicle)
                                }
                                GarageSection(state.vehicles, vm::refreshVehicles) { vehicles ->
                                    if (vehicles.isEmpty()) {
                                        PulseEmptyState(
                                            stringResource(
                                                R.string.pulse_garage_room_for_your_first_ride,
                                            ),
                                            stringResource(
                                                R.string.pulse_garage_add_a_vehicle_to_record_fuel_tire_pressure_and,
                                            ),
                                        )
                                    }
                                    GarageVehicleGrid(
                                        vehicles,
                                        state.vehicle,
                                        !state.busy && state.draft == null,
                                        vm::select,
                                    )
                                }
                            }
                        }
                        val details: @Composable () -> Unit = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
                            ) {
                                if (state.vehicle != null) {
                                    GarageSection(state.history, vm::refreshHistory) { data ->
                                        val vehicle = checkNotNull(state.vehicle)
                                        val remove: (
                                            GarageChange.Delete,
                                            UiText,
                                        ) -> Unit = { change, body ->
                                            deletion =
                                                change to body
                                        }
                                        val edit: (
                                            GarageDraft,
                                        ) -> Unit = {
                                            records = false
                                            vm.edit(it)
                                        }
                                        androidx.compose.runtime.key(vehicle) {
                                            when (state.tab) {
                                                GarageTab.Overview -> {
                                                    VehicleIdentityHeader(data, enabled, { edit(data.draft(vehicle)) }) {
                                                        remove(
                                                            GarageChange.Delete(GarageRecord.Vehicle, vehicle),
                                                            uiText(
                                                                R.string.pulse_garage_delete_1_s_this_permanently_deletes_the_vehicle_fuel,
                                                                data.name,
                                                            ),
                                                        )
                                                    }
                                                    GarageOverview(
                                                        data,
                                                        enabled,
                                                        {
                                                            records = false
                                                            vm.create(GarageForm.Fuel)
                                                        },
                                                        {
                                                            records = false
                                                            vm.create(GarageForm.Care)
                                                        },
                                                        { vm.tab(GarageTab.Care) },
                                                        { vm.tab(GarageTab.Air) },
                                                    )
                                                    VehicleDataCredits()
                                                }
                                                GarageTab.Fuel -> {
                                                    FuelHistoryHeader(data.fuel.size, enabled) {
                                                        records = false
                                                        vm.create(GarageForm.Fuel)
                                                    }
                                                    FuelCards(data, vehicle, enabled, edit, remove)
                                                }
                                                GarageTab.Care -> {
                                                    val schedules =
                                                        data.schedules.filter {
                                                            it.kind != "engine_oil" ||
                                                                data.fuelTypes.any { fuel -> fuel != "electric" }
                                                        }
                                                    when (val page = state.reminderPage) {
                                                        null -> {
                                                            MaintenanceReminderEntry(enabled) { vm.reminders("list") }
                                                            PulseButton(
                                                                stringResource(
                                                                    R.string.pulse_garage_add_maintenance,
                                                                ),
                                                                {
                                                                    records =
                                                                        false
                                                                    vm.create(GarageForm.Care)
                                                                },
                                                                enabled = enabled,
                                                            )
                                                            MaintenanceCards(data, vehicle, enabled, state.busy, edit, remove, upload, {
                                                                record,
                                                                receipt,
                                                                ->
                                                                vm.download(record, receipt)
                                                            }, download, state.uploadRecord, state.progress)
                                                        }
                                                        "list" -> MaintenanceReminderTasks(schedules, enabled, vm::reminders)
                                                        else ->
                                                            schedules.firstOrNull { it.kind == page }?.let { setting ->
                                                                MaintenanceScheduleEditor(
                                                                    setting,
                                                                    enabled,
                                                                    vm::backFromReminder,
                                                                ) { vm.mutate(GarageChange.Schedule(vehicle, it)) }
                                                            }
                                                    }
                                                }
                                                GarageTab.Air -> {
                                                    TiresAirSection(
                                                        data,
                                                        vehicle,
                                                        enabled,
                                                        vm::pressure,
                                                        { vm.mutate(GarageChange.Air(vehicle)) },
                                                        remove,
                                                        { vm.mutate(GarageChange.Air(vehicle, it)) },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (state.vehicle == null) {
                            collection()
                        } else {
                            details()
                        }
                    }
                    if (state.busy &&
                        state.progress == null
                    ) {
                        PulseLoadingState(
                            stringResource(R.string.pulse_garage_working_on_your_garage),
                        )
                    }
                }
                if (fuelEditor) {
                    Box(Modifier.padding(horizontal = size.pagePadding)) {
                        FuelEditorActions(state.busy, state.reviewRequired, vm::save, cancel)
                    }
                }
            }
        }
        Column(Modifier.widthIn(max = PulseTokens.contentWidth).fillMaxSize()) {
            if (state.vehicle != null && state.reminderPage == null && (state.draft == null || records)) {
                PulseTabs(
                    labels = GarageTab.entries.map { stringResource(it.label) },
                    selectedIndex = state.tab.ordinal,
                    onSelect = { vm.tab(GarageTab.entries[it]) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.busy,
                )
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pullToRefresh(
                        isRefreshing = tracker.requested && state.vehicle != null && state.history.loading,
                        state = refreshState,
                        enabled = canRefresh,
                        onRefresh = refresh,
                    ).semantics {
                        customActions =
                            if (canRefresh) {
                                listOf(
                                    CustomAccessibilityAction(refreshLabel) {
                                        refresh()
                                        true
                                    },
                                )
                            } else {
                                emptyList()
                            }
                    },
            ) {
                if (showLoading) {
                    PulseLoadingState(
                        stringResource(R.string.pulse_garage_loading_your_garage),
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .padding(LocalPulseContentPadding.current)
                                .padding(size.pagePadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    )
                } else if (state.draft != null &&
                    !records
                ) {
                    key(state.draft.kind, state.draft.id) { content() }
                } else {
                    scrollStates.SaveableStateProvider("${state.vehicle}-${state.tab}-${state.reminderPage}") {
                        content()
                    }
                }
                if (state.vehicle != null && state.draft == null) {
                    PullToRefreshDefaults.Indicator(
                        state = refreshState,
                        isRefreshing = tracker.requested && state.history.loading,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            }
        }
    }
    if (discard) {
        PulseDialog(
            stringResource(
                R.string.pulse_garage_discard_unsaved_changes,
            ),
            stringResource(
                R.string.pulse_garage_your_edits_have_not_been_saved_discard_this_device,
            ),
            stringResource(R.string.pulse_garage_discard_edits),
            {
                discard =
                    false
                records = false
                vm.discard()
            },
            { discard = false },
        )
    }
    deletion?.let { (change, body) ->
        PulseDialog(
            stringResource(
                R.string.pulse_garage_confirm_deletion,
            ),
            body.asString(),
            stringResource(R.string.pulse_garage_delete),
            {
                deletion =
                    null
                vm.mutate(change)
            },
            { deletion = null },
        )
    }
}

@Composable
private fun <T> GarageSection(
    section: GarageSection<T>,
    retry: () -> Unit,
    content: @Composable (T) -> Unit,
) {
    if (section.loading) {
        if (section.value != null) {
            PulseRefreshIndicator()
        }
    }
    section.error?.let { PulseErrorState(it.message, retry) }
    section.value?.let { content(it) }
}
