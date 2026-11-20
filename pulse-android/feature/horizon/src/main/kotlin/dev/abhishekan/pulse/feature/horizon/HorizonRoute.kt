package dev.abhishekan.pulse.feature.horizon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.PulseHideNavigation
import dev.abhishekan.pulse.core.ui.PulseScreenTransition
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import dev.abhishekan.pulse.feature.horizon.presentation.navigation.HorizonDestination
import dev.abhishekan.pulse.feature.horizon.presentation.navigation.HorizonHost
import dev.abhishekan.pulse.feature.horizon.presentation.navigation.destination
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonAction
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonViewModel
import dev.abhishekan.pulse.feature.horizon.presentation.statementimport.StatementImportAction
import dev.abhishekan.pulse.feature.horizon.presentation.statementimport.StatementImportScreen
import dev.abhishekan.pulse.feature.horizon.presentation.statementimport.StatementImportViewModel
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.recent.RecentTransactionsAction
import org.koin.androidx.compose.koinViewModel

@Composable
fun HorizonRoute(viewModel: HorizonViewModel = koinViewModel()) {
    val accountCurrency by UserCurrency.changes
        .collectAsStateWithLifecycle()
    val timeZone by UserTimeZone.changes.collectAsStateWithLifecycle()
    LaunchedEffect(timeZone) { viewModel.onAction(HorizonAction.Refresh) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val importViewModel: StatementImportViewModel = koinViewModel()
    val destination = state.destination
    PulseHideNavigation(destination !is HorizonDestination.Tabs || state.subscriptionId != null)
    val scrollStates = rememberSaveableStateHolder()
    if (destination != HorizonDestination.StatementImport) {
        PulseSnackbarEffect(state.message) {
            viewModel.onAction(HorizonAction.ConsumeMessage)
        }
    }
    PulseScreenTransition(state, screenDepth = { it.destination.depth }) { screen, active ->
        if (screen.destination == HorizonDestination.StatementImport) {
            StatementImportScreen(
                screen.overview.value
                    ?.categories
                    .orEmpty(),
                onClose = { viewModel.onAction(HorizonAction.CloseImport) },
                onManageAccounts = { viewModel.onAction(HorizonAction.ManageAccounts) },
                onCompleted = { viewModel.onAction(HorizonAction.ImportCompleted) },
                active = active,
                vm = importViewModel,
            )
        } else {
            key(timeZone, accountCurrency) {
                HorizonHost(screen, scrollStates, active) { action ->
                    if (action ==
                        HorizonAction.Recent(RecentTransactionsAction.Import)
                    ) {
                        importViewModel.onAction(StatementImportAction.Reset)
                    }
                    viewModel.onAction(action)
                }
            }
        }
    }
}
