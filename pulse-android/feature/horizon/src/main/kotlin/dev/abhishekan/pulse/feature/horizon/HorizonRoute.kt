package dev.abhishekan.pulse.feature.horizon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.PulseHideNavigation
import dev.abhishekan.pulse.core.ui.PulseScreenTransition
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import org.koin.androidx.compose.koinViewModel

@Composable
fun HorizonRoute(viewModel: HorizonViewModel = koinViewModel()) {
    val accountCurrency by UserCurrency.changes
        .collectAsStateWithLifecycle()
    val timeZone by UserTimeZone.changes.collectAsStateWithLifecycle()
    LaunchedEffect(timeZone) { viewModel.refresh() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val importViewModel: StatementImportViewModel = koinViewModel()
    val importState by importViewModel.uiState.collectAsStateWithLifecycle()
    var importMessage by remember { mutableStateOf<UiText?>(null) }
    LaunchedEffect(state.importOpen, importState.result) {
        if (state.importOpen && importState.result != null) {
            viewModel.setImportOpen(false)
            viewModel.refresh()
            importMessage = uiText(R.string.pulse_horizon_statement_complete)
        }
    }
    val destination = state.destination
    PulseHideNavigation(destination !is HorizonDestination.Tabs || state.subscriptionId != null)
    val scrollStates = rememberSaveableStateHolder()
    if (destination == HorizonDestination.StatementImport) {
        StatementImportScreen(
            state.overview.value
                ?.categories
                .orEmpty(),
            onClose = { viewModel.setImportOpen(false) },
            onManageAccounts = {
                viewModel.setImportOpen(false)
                viewModel.tab(HorizonTab.Accounts)
            },
            vm = importViewModel,
        )
        return
    }
    PulseSnackbarEffect(importMessage) { importMessage = null }
    PulseSnackbarEffect(state.message.takeUnless { state.reviewRequired }, viewModel::consumeMessage)
    PulseScreenTransition(state, screenDepth = { it.destination.depth }) { screen, active ->
        key(timeZone, accountCurrency) {
            HorizonHost(screen, viewModel, scrollStates, active) {
                importViewModel.reset()
                viewModel.setImportOpen(true)
            }
        }
    }
}
