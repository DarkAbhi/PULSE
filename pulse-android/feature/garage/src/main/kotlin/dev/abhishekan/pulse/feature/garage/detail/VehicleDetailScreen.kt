package dev.abhishekan.pulse.feature.garage.detail

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.domain.model.GarageRecord
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseTabs
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.rememberPullRefreshTracker
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.garage.R
import dev.abhishekan.pulse.feature.garage.air.TiresAirSection
import dev.abhishekan.pulse.feature.garage.detail.overview.GarageOverview
import dev.abhishekan.pulse.feature.garage.detail.overview.VehicleIdentityHeader
import dev.abhishekan.pulse.feature.garage.editor.GarageDraft
import dev.abhishekan.pulse.feature.garage.editor.GarageEditorAction
import dev.abhishekan.pulse.feature.garage.editor.GarageEditorLayout
import dev.abhishekan.pulse.feature.garage.editor.GarageEditorState
import dev.abhishekan.pulse.feature.garage.editor.GarageForm
import dev.abhishekan.pulse.feature.garage.editor.draft
import dev.abhishekan.pulse.feature.garage.fuel.FuelCards
import dev.abhishekan.pulse.feature.garage.fuel.FuelHistoryHeader
import dev.abhishekan.pulse.feature.garage.maintenance.MaintenanceCards
import dev.abhishekan.pulse.feature.garage.maintenance.reminders.MaintenanceReminderEntry
import dev.abhishekan.pulse.feature.garage.maintenance.reminders.MaintenanceReminderTasks
import dev.abhishekan.pulse.feature.garage.maintenance.reminders.MaintenanceScheduleEditor
import dev.abhishekan.pulse.feature.garage.maintenance.reminders.ReminderPage
import dev.abhishekan.pulse.feature.garage.maintenance.reminders.editableSchedules
import dev.abhishekan.pulse.feature.garage.shared.GarageSectionContent
import kotlinx.coroutines.flow.first

@Composable
internal fun VehicleDetailScreen(
    state: VehicleDetailUiState,
    editor: GarageEditorState,
    active: Boolean,
    scrollStates: SaveableStateHolder,
    title: String,
    backLabel: String,
    onAction: (VehicleDetailAction) -> Unit,
    onUpload: (Long) -> Unit,
    onDownload: (Long, GarageReceipt) -> Unit,
) {
    val vehicle = state.vehicleId
    val onRefresh = { onAction(VehicleDetailAction.Editor(GarageEditorAction.Refresh)) }
    val onEdit: (GarageDraft) -> Unit = { onAction(VehicleDetailAction.Edit(it)) }
    val onReminder: (ReminderPage) -> Unit = { onAction(VehicleDetailAction.OpenReminders(it)) }
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
    val dispatch by rememberUpdatedState(onAction)
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
                dispatch(VehicleDetailAction.SelectTab(GarageTab.entries[page]))
            }
        }
    }
    val schedules = remember(history) { history?.editableSchedules().orEmpty() }
    val reminder = schedules.firstOrNull { it.kind == state.reminderPage?.savedKey }
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
                        { onAction(VehicleDetailAction.BackFromReminder) },
                    ) { onAction(VehicleDetailAction.Mutate(GarageChange.Schedule(vehicle, it))) }
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
        scrollKey = state.reminderPage?.savedKey ?: selectedPage.toString(),
        onAction = { onAction(VehicleDetailAction.Editor(it)) },
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
                    onSelect = { onAction(VehicleDetailAction.SelectTab(GarageTab.entries[it])) },
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
        GarageSectionContent(state.history, onRefresh) { data ->
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
                        { onAction(VehicleDetailAction.Create(GarageForm.Fuel)) },
                        { onAction(VehicleDetailAction.Create(GarageForm.Care)) },
                        { onAction(VehicleDetailAction.SelectTab(GarageTab.Care)) },
                        { onAction(VehicleDetailAction.SelectTab(GarageTab.Air)) },
                    )
                }
                GarageTab.Fuel -> {
                    FuelHistoryHeader(data.fuel.size, enabled) { onAction(VehicleDetailAction.Create(GarageForm.Fuel)) }
                    FuelCards(data, vehicle, enabled, onEdit, remove)
                }
                GarageTab.Care -> {
                    when (state.reminderPage) {
                        null -> {
                            MaintenanceReminderEntry(enabled) { onReminder(ReminderPage.List) }
                            PulseButton(
                                stringResource(
                                    R.string.pulse_garage_add_maintenance,
                                ),
                                { onAction(VehicleDetailAction.Create(GarageForm.Care)) },
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
                                { record, receipt -> onAction(VehicleDetailAction.DownloadReceipt(record, receipt)) },
                                onDownload,
                                state.uploadRecord,
                                state.progress,
                            )
                        }
                        ReminderPage.List -> MaintenanceReminderTasks(schedules, enabled, onReminder)
                        else -> Unit
                    }
                }
                GarageTab.Air -> {
                    TiresAirSection(
                        data,
                        vehicle,
                        enabled,
                        { onAction(VehicleDetailAction.EditPressure) },
                        { onAction(VehicleDetailAction.Mutate(GarageChange.Air(vehicle))) },
                        remove,
                        { onAction(VehicleDetailAction.Mutate(GarageChange.Air(vehicle, it))) },
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
                onAction(VehicleDetailAction.Mutate(change))
            },
            { deletion = null },
        )
    }
}
