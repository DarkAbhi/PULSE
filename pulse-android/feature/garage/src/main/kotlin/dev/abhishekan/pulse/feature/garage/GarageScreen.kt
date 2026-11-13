package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.ui.PulseHideNavigation
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import org.koin.androidx.compose.koinViewModel

@Composable
fun GarageRoute(onOpenVehicle: (Long) -> Unit) {
    val viewModel: GarageViewModel = koinViewModel()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val editor by viewModel.editorState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.resume() }
    PulseHideNavigation(editor.draft != null)
    PulseSnackbarEffect(editor.message.takeUnless { editor.reviewRequired }, viewModel::consumeMessage)

    GarageEditorLayout(
        editor = editor,
        title = stringResource(R.string.pulse_garage_screen_title),
        backLabel = null,
        loading = vehicles.loading && vehicles.value == null,
        reviewReady = vehicles.ready,
        scrollKey = "collection",
        onRefresh = viewModel::refresh,
        onReview = viewModel::acknowledgeReview,
        onDiscard = viewModel::discard,
        onUpdate = viewModel::update,
        onSave = viewModel::save,
        onRetryCatalog = viewModel::retryCatalog,
    ) { enabled, _ ->
        Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
            GarageCollectionHeader(enabled, viewModel::addVehicle)
            GarageSection(vehicles, viewModel::refresh) { items ->
                if (items.isEmpty()) {
                    PulseEmptyState(
                        stringResource(R.string.pulse_garage_room_for_your_first_ride),
                        stringResource(R.string.pulse_garage_add_a_vehicle_to_record_fuel_tire_pressure_and),
                    )
                }
                GarageVehicleGrid(items, selected = null, enabled = !editor.busy && editor.draft == null, select = onOpenVehicle)
            }
        }
    }
}
