package dev.abhishekan.pulse.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

internal fun NavController.navigateSingleTop(route: String) = navigate(route) { launchSingleTop = true }

internal fun isTabTransition(
    from: NavBackStackEntry,
    to: NavBackStackEntry,
): Boolean = from.isTabRoot() && to.isTabRoot()

private fun NavBackStackEntry.isTabRoot(): Boolean =
    isTabRoot(
        AppDestination.fromRoute(destination.route),
        editName = arguments?.getBoolean("editName") == true,
        fromHome = arguments?.getBoolean("fromHome") == true,
    )

internal fun isTabRoot(
    destination: AppDestination?,
    editName: Boolean,
    fromHome: Boolean = false,
): Boolean = destination in AppDestination.tabs && !(destination == AppDestination.Profile && (editName || fromHome))

internal fun NavController.navigateToTab(destination: AppDestination) =
    navigate(destination.path) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = destination != AppDestination.Home
    }
