package dev.abhishekan.pulse.shell

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.abhishekan.pulse.R
import dev.abhishekan.pulse.appearance.AppearanceUiState
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.ui.DraftSource
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.navigation.AppDestination
import dev.abhishekan.pulse.navigation.SignedInNavHost
import dev.abhishekan.pulse.navigation.navigateToTab
import dev.abhishekan.pulse.session.SessionOperation
import dev.abhishekan.pulse.feature.profile.R as FeatureProfileR

@Composable
internal fun SignedInApp(
    username: String,
    appearance: AppearanceUiState,
    onTheme: (ThemePreference) -> Unit,
    operation: SessionOperation,
    onLogout: () -> Unit,
    clearError: () -> Unit,
    unsavedChanges: Set<DraftSource>,
) {
    val nav = rememberNavController()
    val shell = rememberSaveable(saver = ShellState.Saver) { ShellState() }
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    val entry by nav.currentBackStackEntryAsState()
    val destination = AppDestination.fromRoute(entry?.destination?.route)
    val selectDestination: (AppDestination) -> Unit = { target ->
        if (DraftSource.Profile in unsavedChanges && destination == AppDestination.Profile && target != AppDestination.Profile) {
            shell.requestLeave(target)
        } else {
            nav.navigateToTab(target)
        }
    }
    val backToNotifications =
        if (nav.previousBackStackEntry?.destination?.route == AppDestination.Notifications.route) {
            stringResource(R.string.pulse_app_back_to_notifications)
        } else {
            null
        }

    val snackbar = remember { SnackbarHostState() }
    val operationMessage = operation.error?.asString()
    LaunchedEffect(operationMessage) {
        operationMessage?.let {
            snackbar.showSnackbar(it)
            clearError()
        }
    }

    SignedInScaffold(
        destination = destination,
        isTablet = isTablet,
        snackbar = snackbar,
        selectDestination = selectDestination,
    ) { modifier ->
        SignedInNavHost(
            nav = nav,
            username = username,
            shell = shell,
            isTablet = isTablet,
            appearance = appearance,
            onTheme = onTheme,
            loggingOut = operation.running,
            otherDraftsOpen = unsavedChanges.any { it != DraftSource.Profile },
            onLogout = onLogout,
            onSelectTab = selectDestination,
            backToNotifications = backToNotifications,
            modifier = modifier,
        )
    }

    shell.pendingDestination?.let {
        PulseDialog(
            stringResource(FeatureProfileR.string.pulse_profile_discard_title),
            stringResource(FeatureProfileR.string.pulse_profile_discard_body),
            stringResource(FeatureProfileR.string.pulse_profile_discard),
            { shell.confirmLeave()?.let(nav::navigateToTab) },
            shell::cancelLeave,
        )
    }
}
