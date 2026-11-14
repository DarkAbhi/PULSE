package dev.abhishekan.pulse

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import dev.abhishekan.pulse.core.ui.R as UiR

internal enum class AppDestination(
    val route: String,
    @param:StringRes val label: Int = R.string.pulse_app_pulse,
    @param:DrawableRes val icon: Int? = null,
) {
    Home("home", R.string.pulse_app_home, UiR.drawable.pulse_home),
    Fitness("fitness", R.string.pulse_app_fitness, UiR.drawable.pulse_fitness),
    Horizon("horizon", R.string.pulse_app_horizon, UiR.drawable.pulse_horizon),
    Garage("garage", R.string.pulse_app_garage, UiR.drawable.pulse_garage),
    MealPlan("mealplan", R.string.pulse_app_meals, UiR.drawable.pulse_meals),
    Profile("you?editName={editName}&fromHome={fromHome}", R.string.pulse_app_you, UiR.drawable.pulse_person),
    Workout("workout/{id}"),
    Vehicle("vehicle/{id}"),
    Notifications("notifications"),
    Gallery("gallery"),
    ;

    val path get() = route.substringBefore('?')
    val tab get() =
        when (this) {
            Workout -> Fitness
            Vehicle -> Garage
            Notifications -> Home
            else -> this
        }

    fun withId(id: Long) = route.replace("{id}", id.toString())

    companion object {
        val tabs = entries.filter { it.icon != null }

        fun fromRoute(route: String?) = entries.find { it.route == route }
    }
}

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
