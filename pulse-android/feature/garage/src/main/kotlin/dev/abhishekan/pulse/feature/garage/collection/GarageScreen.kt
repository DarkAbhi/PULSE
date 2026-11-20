package dev.abhishekan.pulse.feature.garage.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.Vehicle
import dev.abhishekan.pulse.core.ui.PulseScreenTransition
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.feature.garage.R
import dev.abhishekan.pulse.feature.garage.editor.GarageEditorAction
import dev.abhishekan.pulse.feature.garage.editor.GarageEditorLayout
import dev.abhishekan.pulse.feature.garage.editor.GarageEditorState
import dev.abhishekan.pulse.feature.garage.shared.GarageSection
import dev.abhishekan.pulse.feature.garage.shared.GarageSectionContent

@Composable
internal fun GarageScreen(
    vehicles: GarageSection<List<Vehicle>>,
    editor: GarageEditorState,
    onAction: (GarageAction) -> Unit,
    onOpenVehicle: (Long) -> Unit,
) {
    val scrollStates = rememberSaveableStateHolder()
    PulseScreenTransition(editor, screenDepth = { if (it.draft == null) 0 else 1 }) { screen, active ->
        GarageEditorLayout(
            editor = screen,
            active = active,
            scrollStates = scrollStates,
            title = stringResource(R.string.pulse_garage_screen_title),
            backLabel = null,
            loading = vehicles.loading && vehicles.value == null,
            reviewReady = vehicles.ready,
            scrollKey = "collection",
            onAction = { onAction(GarageAction.Editor(it)) },
        ) { enabled, _ ->
            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                GarageCollectionHeader(enabled) { onAction(GarageAction.AddVehicle) }
                GarageSectionContent(vehicles, { onAction(GarageAction.Editor(GarageEditorAction.Refresh)) }) { items ->
                    if (items.isEmpty()) {
                        PulseEmptyState(
                            stringResource(R.string.pulse_garage_room_for_your_first_ride),
                            stringResource(R.string.pulse_garage_add_a_vehicle_to_record_fuel_tire_pressure_and),
                        )
                    }
                    GarageVehicleGrid(items, selected = null, enabled = enabled, select = onOpenVehicle)
                }
            }
        }
    }
}
