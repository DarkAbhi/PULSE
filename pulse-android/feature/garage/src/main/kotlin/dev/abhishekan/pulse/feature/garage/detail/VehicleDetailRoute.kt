package dev.abhishekan.pulse.feature.garage.detail

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.PulseHideNavigation
import dev.abhishekan.pulse.core.ui.PulseScreenTransition
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.feature.garage.R
import dev.abhishekan.pulse.feature.garage.detail.receipts.rememberReceiptPickerActions
import dev.abhishekan.pulse.feature.garage.editor.GarageEditorAction
import dev.abhishekan.pulse.feature.garage.maintenance.reminders.ReminderPage
import dev.abhishekan.pulse.feature.garage.maintenance.reminders.reminderPageTitle
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
    ) {
        viewModel.onAction(VehicleDetailAction.Editor(GarageEditorAction.ConsumeMessage))
    }
    val navigateBack by rememberUpdatedState(onUnavailable)
    LaunchedEffect(leave) { if (leave) navigateBack() }
    BackHandler(editor.draft == null && !editor.busy && state.reminderPage != null) {
        viewModel.onAction(VehicleDetailAction.BackFromReminder)
    }

    val receipts =
        rememberReceiptPickerActions(
            events = viewModel.events,
            onAction = viewModel::onAction,
        )

    val scrollStates = rememberSaveableStateHolder()
    PulseScreenTransition(
        state to editor,
        screenDepth = { (detail, edit) ->
            val reminderDepth =
                when (detail.reminderPage) {
                    null -> 0
                    ReminderPage.List -> 1
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
                ReminderPage.List -> stringResource(R.string.pulse_garage_reminder_back_maintenance)
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
                onAction = viewModel::onAction,
                onUpload = receipts.upload,
                onDownload = receipts.download,
            )
        }
    }
}
