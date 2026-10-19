package dev.abhishekan.pulse.feature.garage
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.domain.model.GarageRecord
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseFilterChip
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseRefreshIndicator
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.uiText
import org.koin.androidx.compose.koinViewModel

@Composable
fun GarageRoute(
    onDraftChanged: (Boolean) -> Unit,
    viewModel: GarageViewModel = koinViewModel(),
    initialVehicle: Long? = null,
    onDetailChanged: (Boolean) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
    LaunchedEffect(state.draft?.dirty) { onDraftChanged(state.draft?.dirty == true) }
    LaunchedEffect(state.vehicle) { onDetailChanged(state.vehicle != null) }
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
            else -> vm.select(null)
        }
    }
    val scrollStates = rememberSaveableStateHolder()
    val enabled = !state.busy && !state.reviewRequired && state.draft == null
    val refreshState = rememberPullToRefreshState()
    val canRefresh = state.vehicle != null && !state.history.loading && !state.busy && state.draft == null
    val showLoading =
        !state.reviewRequired &&
            (state.draft == null || records) &&
            if (state.vehicle == null) state.vehicles.loading else state.history.loading
    val refresh = { if (canRefresh) vm.refreshHistory() }
    val refreshLabel = stringResource(R.string.pulse_garage_refresh_vehicle_history)
    PulseAdaptiveLayout(Modifier.fillMaxSize()) { size ->
        val content: @Composable () -> Unit = {
            val fuelEditor = state.draft?.kind == GarageForm.Fuel && !records
            Column(
                Modifier.fillMaxSize().padding(LocalPulseContentPadding.current).imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
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
                            PulseTextButton(
                                stringResource(R.string.pulse_garage_refresh_server_records),
                                vm::refresh,
                                enabled = !state.busy,
                            )
                            if (state.draft !=
                                null
                            ) {
                                PulseTextButton(
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
                                )
                            }
                            PulseButton(
                                stringResource(R.string.pulse_garage_i_reviewed_the_server_records),
                                vm::acknowledgeReview,
                                enabled =
                                    state.reviewReady && !state.busy,
                            )
                        }
                    } else {
                        state.message?.let {
                            PulseText(
                                it,
                                Modifier.semantics {
                                    liveRegion =
                                        LiveRegionMode.Polite
                                },
                                color = PulseTheme.colors.textSecondary,
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
                        )
                    } else {
                        if (state.draft !=
                            null
                        ) {
                            PulseTextButton(
                                stringResource(
                                    R.string.pulse_garage_resume_unsaved_edits,
                                ),
                                {
                                    records =
                                        false
                                },
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
                                        PulseText(
                                            data.name,
                                            Modifier.semantics {
                                                heading()
                                            },
                                            PulseTextStyle.Display,
                                        )
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
                                        when (state.tab) {
                                            GarageTab.Overview -> {
                                                EfficiencyCards(data)
                                                PulseCard(Modifier.fillMaxWidth()) {
                                                    PulseText(
                                                        stringResource(
                                                            R.string.pulse_garage_care_at_a_glance,
                                                        ),
                                                        style = PulseTextStyle.Title,
                                                    )
                                                    PulseText(
                                                        stringResource(
                                                            R.string.pulse_garage_history_counts,
                                                            pluralStringResource(
                                                                R.plurals.pulse_garage_fuel_count,
                                                                data.fuel.size,
                                                                data.fuel.size,
                                                            ),
                                                            pluralStringResource(
                                                                R.plurals.pulse_garage_maintenance_count,
                                                                data.maintenance.size,
                                                                data.maintenance.size,
                                                            ),
                                                            pluralStringResource(
                                                                R.plurals.pulse_garage_air_count,
                                                                data.air.size,
                                                                data.air.size,
                                                            ),
                                                        ),
                                                    )
                                                    data.maintenance.firstOrNull()?.let {
                                                        PulseText(
                                                            stringResource(
                                                                R.string.pulse_garage_latest_care_1_s_2_s,
                                                                it.title,
                                                                garageDate(it.date),
                                                            ),
                                                        )
                                                    }
                                                    PulseButton(
                                                        stringResource(
                                                            R.string.pulse_garage_add_fuel_fill_up,
                                                        ),
                                                        {
                                                            records =
                                                                false
                                                            vm.create(GarageForm.Fuel)
                                                        },
                                                        enabled = enabled,
                                                    )
                                                    PulseTextButton(
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
                                                }
                                                PressureCard(data.pressure, enabled, vm::pressure, {
                                                    vm.mutate(GarageChange.Air(vehicle))
                                                })
                                                PulseTextButton(
                                                    stringResource(
                                                        R.string.pulse_garage_delete_1_s,
                                                        data.name,
                                                    ),
                                                    {
                                                        remove(
                                                            GarageChange.Delete(
                                                                GarageRecord.Vehicle,
                                                                vehicle,
                                                            ),
                                                            uiText(
                                                                R.string.pulse_garage_delete_1_s_this_permanently_deletes_the_vehicle_fuel,
                                                                data.name,
                                                            ),
                                                        )
                                                    },
                                                    enabled = enabled,
                                                )
                                            }
                                            GarageTab.Fuel -> {
                                                PulseButton(
                                                    stringResource(
                                                        R.string.pulse_garage_add_fuel_fill_up,
                                                    ),
                                                    {
                                                        records =
                                                            false
                                                        vm.create(GarageForm.Fuel)
                                                    },
                                                    enabled = enabled,
                                                )
                                                FuelCards(data, vehicle, enabled, edit, remove)
                                            }
                                            GarageTab.Care -> {
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
                                            GarageTab.Air -> {
                                                PressureCard(data.pressure, enabled, vm::pressure, {
                                                    vm.mutate(GarageChange.Air(vehicle))
                                                })
                                                if (data.air.isEmpty()) {
                                                    PulseEmptyState(
                                                        stringResource(
                                                            R.string.pulse_garage_no_air_fills_recorded,
                                                        ),
                                                        stringResource(
                                                            R.string.pulse_garage_record_an_air_fill_after_checking_your_tires,
                                                        ),
                                                    )
                                                }
                                                data.air.forEach { air ->
                                                    PulseCard(Modifier.fillMaxWidth()) {
                                                        PulseText(
                                                            garageDate(air.date),
                                                            style = PulseTextStyle.Title,
                                                        )
                                                        PulseTextButton(
                                                            stringResource(
                                                                R.string.pulse_garage_delete_air_fill_1_s,
                                                                air.id,
                                                            ),
                                                            {
                                                                remove(
                                                                    GarageChange.Delete(
                                                                        GarageRecord.Air,
                                                                        vehicle,
                                                                        air.id,
                                                                    ),
                                                                    uiText(
                                                                        R.string.pulse_garage_delete_air_fill_confirmation,
                                                                    ),
                                                                )
                                                            },
                                                            enabled = enabled,
                                                        )
                                                    }
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
                    androidx.compose.foundation.layout.Box(Modifier.padding(horizontal = size.pagePadding)) {
                        FuelEditorActions(state.busy, state.reviewRequired, vm::save, cancel)
                    }
                }
            }
        }
        Column(Modifier.widthIn(max = PulseTokens.contentWidth).fillMaxSize()) {
            if (state.vehicle != null && (state.draft == null || records)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(
                            rememberScrollState(),
                        ).padding(start = size.pagePadding, top = size.pagePadding, end = size.pagePadding),
                    horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
                ) {
                    GarageTab.entries.forEach { tab ->
                        PulseFilterChip(
                            stringResource(tab.label),
                            state.tab == tab,
                            { vm.tab(tab) },
                            enabled = !state.busy,
                        )
                    }
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pullToRefresh(
                        isRefreshing = state.vehicle != null && state.history.loading,
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
                    scrollStates.SaveableStateProvider("${state.vehicle}-${state.tab}") {
                        content()
                    }
                }
                if (state.vehicle != null && state.draft == null) {
                    PullToRefreshDefaults.Indicator(
                        state = refreshState,
                        isRefreshing = state.history.loading,
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
        if (section.value == null) {
            PulseLoadingState(stringResource(R.string.pulse_garage_loading_your_garage))
        } else {
            PulseRefreshIndicator()
        }
    }
    section.error?.let { PulseErrorState(it.message, retry) }
    section.value?.let { content(it) }
}
