package dev.abhishekan.pulse.navigation

import androidx.compose.runtime.State
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.abhishekan.pulse.feature.garage.collection.GarageRoute
import dev.abhishekan.pulse.feature.garage.detail.VehicleDetailRoute

internal fun NavGraphBuilder.garageDestinations(
    nav: NavHostController,
    backToNotifications: State<String?>,
) {
    composable(AppDestination.Garage.route) { garage ->
        GarageRoute { id ->
            if (nav.currentBackStackEntry == garage) nav.navigate(AppDestination.Vehicle.withId(id))
        }
    }
    composable(
        AppDestination.Vehicle.route,
        arguments = listOf(navArgument("id") { type = NavType.LongType }),
    ) { vehicle ->
        VehicleDetailRoute(
            onUnavailable = {
                if (nav.currentBackStackEntry == vehicle) {
                    val openedFromNotifications = nav.previousBackStackEntry?.destination?.route == AppDestination.Notifications.route
                    val popped = nav.popBackStack()
                    if (popped && openedFromNotifications) nav.navigateSingleTop(AppDestination.Garage.path)
                }
            },
            backLabel = backToNotifications.value,
        )
    }
}
