package dev.abhishekan.pulse.feature.garage.collection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.ui.PulseHideNavigation
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import dev.abhishekan.pulse.feature.garage.editor.GarageEditorAction
import org.koin.androidx.compose.koinViewModel

@Composable
fun GarageRoute(onOpenVehicle: (Long) -> Unit) {
    val viewModel: GarageViewModel = koinViewModel()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val editor by viewModel.editorState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onAction(GarageAction.Resume) }
    PulseHideNavigation(editor.draft != null)
    PulseSnackbarEffect(editor.message.takeUnless { editor.reviewRequired }) {
        viewModel.onAction(GarageAction.Editor(GarageEditorAction.ConsumeMessage))
    }

    GarageScreen(
        vehicles = vehicles,
        editor = editor,
        onAction = viewModel::onAction,
        onOpenVehicle = onOpenVehicle,
    )
}
