package dev.abhishekan.pulse.feature.garage

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.domain.model.GarageRecord
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseHideNavigation
import dev.abhishekan.pulse.core.ui.PulseScreenTransition
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import dev.abhishekan.pulse.core.ui.components.PulseTabs
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.rememberPullRefreshTracker
import dev.abhishekan.pulse.core.ui.uiText
import kotlinx.coroutines.flow.first
import org.koin.androidx.compose.koinViewModel

@Composable
fun VehicleDetailRoute(
    onUnavailable: () -> Unit,
    backLabel: String? = null,
) {
    val viewModel: VehicleDetailViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editorState.collectAsStateWithLifecycle()
    val currency by UserCurrency.changes.collectAsStateWithLifecycle()
    val timeZone by UserTimeZone.changes.collectAsStateWithLifecycle()
    PulseHideNavigation(true)
    val missing = state.history.error == Failure.NotFound
    val leave = (state.deleted || missing) && editor.draft == null && !editor.busy
    PulseSnackbarEffect(
        if (missing && leave) Failure.NotFound.message else editor.message.takeUnless { editor.reviewRequired },
        viewModel::consumeMessage,
    )
    LaunchedEffect(leave) { if (leave) onUnavailable() }
    BackHandler(editor.draft == null && !editor.busy && state.reminderPage != null, viewModel::backFromReminder)

    val context = LocalContext.current
    val openReceiptTitle = stringResource(R.string.pulse_garage_open_receipt)
    var pickerRecord by rememberSaveable { mutableStateOf<Long?>(null) }
    var downloadRecord by rememberSaveable { mutableStateOf<Long?>(null) }
    var downloadReceipt by rememberSaveable { mutableStateOf<List<String>?>(null) }
    val pick =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val record = pickerRecord
            if (uri != null && record != null) viewModel.upload(record, uri.toString())
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
                )
            }
            downloadRecord = null
            downloadReceipt = null
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

    val scrollStates = rememberSaveableStateHolder()
    PulseScreenTransition(
        state to editor,
        screenDepth = { (detail, edit) ->
            val reminderDepth =
                when (detail.reminderPage) {
                    null -> 0
                    "list" -> 1
                    else -> 2
                }
            reminderDepth + if (edit.draft == null) 0 else 1
        },
    ) { (detail, edit), active ->
        val title =
            detail.reminderPage?.let { reminderPageTitle(it) }
                ?: detail.history.value?.name
                ?: stringResource(R.string.pulse_garage_screen_title)
        val detailBackLabel =
            when (detail.reminderPage) {
                null -> backLabel ?: stringResource(R.string.pulse_garage_back_to_collection)
                "list" -> stringResource(R.string.pulse_garage_reminder_back_maintenance)
                else -> stringResource(R.string.pulse_garage_reminder_back_settings)
            }

        key(timeZone, currency) {
            VehicleDetailScreen(
                state = detail,
                editor = edit,
                active = active,
                scrollStates = scrollStates,
                title = title,
                backLabel = detailBackLabel,
                onRefresh = viewModel::refresh,
                onReview = viewModel::acknowledgeReview,
                onDiscard = viewModel::discard,
                onUpdate = viewModel::update,
                onSave = viewModel::save,
                onRetryCatalog = viewModel::retryCatalog,
                onCreate = viewModel::create,
                onEdit = viewModel::edit,
                onTab = viewModel::tab,
                onReminder = viewModel::reminders,
                onBackFromReminder = viewModel::backFromReminder,
                onPressure = viewModel::pressure,
                onMutate = viewModel::mutate,
                onOpenReceipt = { record, receipt -> viewModel.download(record, receipt) },
                onUpload = { record ->
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
                },
                onDownload = { record, receipt ->
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
                },
            )
        }
    }
}

@Composable
private fun VehicleDetailScreen(
    state: VehicleDetailUiState,
    editor: GarageEditorState,
    active: Boolean,
    scrollStates: SaveableStateHolder,
    title: String,
    backLabel: String,
    onRefresh: () -> Unit,
    onReview: () -> Unit,
    onDiscard: () -> Unit,
    onUpdate: (GarageDraft) -> Unit,
    onSave: () -> Unit,
    onRetryCatalog: () -> Unit,
    onCreate: (GarageForm) -> Unit,
    onEdit: (GarageDraft) -> Unit,
    onTab: (GarageTab) -> Unit,
    onReminder: (String?) -> Unit,
    onBackFromReminder: () -> Unit,
    onPressure: () -> Unit,
    onMutate: (GarageChange) -> Unit,
    onOpenReceipt: (Long, GarageReceipt) -> Unit,
    onUpload: (Long) -> Unit,
    onDownload: (Long, GarageReceipt) -> Unit,
) {
    val vehicle = state.vehicleId
    var deletion by remember { mutableStateOf<Pair<GarageChange.Delete, UiText>?>(null) }
    val remove: (GarageChange.Delete, UiText) -> Unit = { change, body -> deletion = change to body }
    val refreshState = rememberPullToRefreshState()
    val tracker = rememberPullRefreshTracker(state.history.loading)
    val pullRefreshing = tracker.requested && state.history.loading
    val canRefresh =
        state.reminderPage == null && !state.history.loading && !tracker.requested && !editor.busy && editor.draft == null
    val refresh = { if (canRefresh) tracker.request(onRefresh) }
    val refreshLabel = stringResource(R.string.pulse_garage_refresh_vehicle_history)
    val history = state.history.value
    val selectTab by rememberUpdatedState(onTab)
    val selectedPage = state.tab.ordinal
    val pager = remember { PagerState(currentPage = selectedPage) { GarageTab.entries.size } }
    val showPager = state.reminderPage == null && editor.draft == null
    LaunchedEffect(pager, selectedPage, showPager, editor.busy, active) {
        if (active && showPager) {
            if (pager.currentPage != selectedPage || pager.currentPageOffsetFraction != 0f || pager.isScrollInProgress) {
                pager.scrollToPage(selectedPage)
            }
            if (!editor.busy) {
                val page = snapshotFlow { pager.settledPage }.first { it != selectedPage }
                selectTab(GarageTab.entries[page])
            }
        }
    }
    val reminder =
        history?.schedules?.firstOrNull {
            it.kind == state.reminderPage && (it.kind != "engine_oil" || history.fuelTypes.any { fuel -> fuel != "electric" })
        }
    val reminderEditor: (@Composable (Boolean, @Composable () -> Unit) -> Unit)? =
        reminder?.let { setting ->
            { enabled, prefix ->
                PulseAdaptiveLayout(Modifier.fillMaxSize().padding(LocalPulseContentPadding.current).imePadding()) { size ->
                    MaintenanceScheduleEditor(
                        setting,
                        enabled,
                        editor.busy,
                        size.pagePadding,
                        {
                            prefix()
                            state.history.error?.let { PulseErrorState(it.message, onRefresh) }
                        },
                        onBackFromReminder,
                    ) { onMutate(GarageChange.Schedule(vehicle, it)) }
                }
            }
        }

    GarageEditorLayout(
        editor = editor,
        active = active,
        scrollStates = scrollStates,
        title = title,
        backLabel = backLabel,
        loading = state.history.loading && state.history.value == null && !pullRefreshing,
        reviewReady = state.history.ready,
        scrollKey = state.reminderPage ?: selectedPage.toString(),
        onRefresh = onRefresh,
        onReview = onReview,
        onDiscard = onDiscard,
        onUpdate = onUpdate,
        onSave = onSave,
        onRetryCatalog = onRetryCatalog,
        showBusyIndicator = state.progress == null,
        recordsEditor = reminderEditor,
        recordsPager = pager.takeIf { showPager },
        bodyModifier =
            Modifier
                .pullToRefresh(
                    isRefreshing = pullRefreshing,
                    state = refreshState,
                    enabled = canRefresh,
                    onRefresh = refresh,
                ).semantics {
                    if (canRefresh) {
                        customActions =
                            listOf(
                                CustomAccessibilityAction(refreshLabel) {
                                    refresh()
                                    true
                                },
                            )
                    }
                },
        header = { showingRecords ->
            if (state.reminderPage == null && showingRecords) {
                PulseTabs(
                    labels = GarageTab.entries.map { stringResource(it.label) },
                    selectedIndex = state.tab.ordinal,
                    onSelect = { onTab(GarageTab.entries[it]) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !editor.busy,
                )
            }
        },
        overlay = {
            if (editor.draft == null) {
                PullToRefreshDefaults.Indicator(
                    state = refreshState,
                    isRefreshing = pullRefreshing,
                    modifier =
                        Modifier.align(Alignment.TopCenter).then(
                            if (pullRefreshing) Modifier.testTag("garage-history-refreshing") else Modifier,
                        ),
                )
            }
        },
    ) { enabled, page ->
        GarageSection(state.history, onRefresh) { data ->
            when (GarageTab.entries[page ?: selectedPage]) {
                GarageTab.Overview -> {
                    VehicleIdentityHeader(data, enabled, { onEdit(data.draft(vehicle)) }) {
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
                        { onCreate(GarageForm.Fuel) },
                        { onCreate(GarageForm.Care) },
                        { onTab(GarageTab.Care) },
                        { onTab(GarageTab.Air) },
                    )
                }
                GarageTab.Fuel -> {
                    FuelHistoryHeader(data.fuel.size, enabled) { onCreate(GarageForm.Fuel) }
                    FuelCards(data, vehicle, enabled, onEdit, remove)
                }
                GarageTab.Care -> {
                    val schedules =
                        data.schedules.filter {
                            it.kind != "engine_oil" ||
                                data.fuelTypes.any { fuel -> fuel != "electric" }
                        }
                    when (state.reminderPage) {
                        null -> {
                            MaintenanceReminderEntry(enabled) { onReminder("list") }
                            PulseButton(
                                stringResource(
                                    R.string.pulse_garage_add_maintenance,
                                ),
                                { onCreate(GarageForm.Care) },
                                enabled = enabled,
                            )
                            MaintenanceCards(
                                data,
                                vehicle,
                                enabled,
                                editor.busy,
                                onEdit,
                                remove,
                                onUpload,
                                onOpenReceipt,
                                onDownload,
                                state.uploadRecord,
                                state.progress,
                            )
                        }
                        "list" -> MaintenanceReminderTasks(schedules, enabled, onReminder)
                        else -> Unit
                    }
                }
                GarageTab.Air -> {
                    TiresAirSection(
                        data,
                        vehicle,
                        enabled,
                        onPressure,
                        { onMutate(GarageChange.Air(vehicle)) },
                        remove,
                        { onMutate(GarageChange.Air(vehicle, it)) },
                    )
                }
            }
        }
    }
    deletion?.takeIf { active }?.let { (change, body) ->
        PulseDialog(
            stringResource(R.string.pulse_garage_confirm_deletion),
            body.asString(),
            stringResource(R.string.pulse_garage_delete),
            {
                deletion = null
                onMutate(change)
            },
            { deletion = null },
        )
    }
}
