package dev.abhishekan.pulse.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.abhishekan.pulse.R
import dev.abhishekan.pulse.appearance.AppearanceUiState
import dev.abhishekan.pulse.core.domain.model.NotificationTarget
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.ui.PulseActivityTransitions
import dev.abhishekan.pulse.debugtools.DebugGalleryAction
import dev.abhishekan.pulse.debugtools.debugGalleryDestination
import dev.abhishekan.pulse.feature.home.HomeRoute
import dev.abhishekan.pulse.feature.horizon.HorizonRoute
import dev.abhishekan.pulse.feature.mealplan.MealPlanRoute
import dev.abhishekan.pulse.feature.notifications.NotificationsRoute
import dev.abhishekan.pulse.feature.profile.ProfileRoute
import dev.abhishekan.pulse.feature.profile.ProfileSetupPrompt
import dev.abhishekan.pulse.shell.OnboardingPrompt
import dev.abhishekan.pulse.shell.ShellState

@Composable
internal fun SignedInNavHost(
    nav: NavHostController,
    username: String,
    shell: ShellState,
    isTablet: Boolean,
    appearance: AppearanceUiState,
    onTheme: (ThemePreference) -> Unit,
    loggingOut: Boolean,
    otherDraftsOpen: Boolean,
    onLogout: () -> Unit,
    onSelectTab: (AppDestination) -> Unit,
    backToNotifications: String?,
    modifier: Modifier = Modifier,
) {
    // Keep route registration stable while destination content reads the latest shell inputs.
    val tablet by rememberUpdatedState(isTablet)
    val currentAppearance = rememberUpdatedState(appearance)
    val updateTheme by rememberUpdatedState(onTheme)
    val logoutRunning by rememberUpdatedState(loggingOut)
    val hasOtherDrafts by rememberUpdatedState(otherDraftsOpen)
    val logout by rememberUpdatedState(onLogout)
    val selectTab by rememberUpdatedState(onSelectTab)
    val notificationBackLabel = rememberUpdatedState(backToNotifications)
    val transitions = PulseActivityTransitions(LocalLayoutDirection.current)
    val backEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isTabTransition(initialState, targetState)) {
            EnterTransition.None
        } else {
            transitions.backEnter
        }
    }
    val backExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isTabTransition(initialState, targetState)) {
            ExitTransition.None
        } else {
            transitions.backExit
        }
    }
    NavHost(
        nav,
        startDestination = AppDestination.Home.route,
        modifier = modifier,
        enterTransition = {
            if (isTabTransition(initialState, targetState)) {
                EnterTransition.None
            } else {
                transitions.forwardEnter
            }
        },
        exitTransition = {
            if (isTabTransition(initialState, targetState)) {
                ExitTransition.None
            } else {
                transitions.forwardExit
            }
        },
        popEnterTransition = backEnter,
        popExitTransition = backExit,
        predictivePopEnterTransition = { backEnter() },
        predictivePopExitTransition = { backExit() },
    ) {
        composable(AppDestination.Home.route) { home ->
            HomeRoute(
                username,
                { selectTab(AppDestination.Horizon) },
                onFitness = { selectTab(AppDestination.Fitness) },
                onNotifications = { nav.navigateSingleTop(AppDestination.Notifications.path) },
                onProfile =
                    if (tablet) {
                        null
                    } else {
                        {
                            if (nav.currentBackStackEntry == home) {
                                nav.navigateSingleTop("${AppDestination.Profile.path}?fromHome=true")
                            }
                        }
                    },
                onMissingProfile = shell::offerOnboarding,
            )
            if (shell.onboarding == OnboardingPrompt.Showing) {
                ProfileSetupPrompt({
                    shell.dismissOnboarding()
                    nav.navigateSingleTop("${AppDestination.Profile.path}?editName=true")
                }, shell::dismissOnboarding)
            }
        }
        composable(AppDestination.MealPlan.route) {
            MealPlanRoute()
        }
        garageDestinations(nav, notificationBackLabel)
        composable(AppDestination.Horizon.route) {
            HorizonRoute()
        }
        fitnessDestinations(nav, notificationBackLabel)
        composable(
            AppDestination.Profile.route,
            arguments =
                listOf(
                    navArgument("editName") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("fromHome") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
        ) { profileEntry ->
            ProfileRoute(
                username,
                currentAppearance.value.theme,
                currentAppearance.value.error,
                { updateTheme(it) },
                logoutRunning,
                hasOtherDrafts,
                { logout() },
                profileEntry.arguments?.getBoolean("editName") == true,
                backLabel = if (tablet) null else stringResource(R.string.pulse_app_back_to_home),
            ) { DebugGalleryAction { nav.navigateSingleTop(AppDestination.Gallery.path) } }
        }
        composable(AppDestination.Notifications.route) { notifications ->
            NotificationsRoute(
                onOpen = { target ->
                    // Keep Notifications underneath the destination it opened.
                    val route =
                        when (target) {
                            NotificationTarget.Fitness -> AppDestination.Fitness.path
                            NotificationTarget.Horizon -> AppDestination.Horizon.path
                            NotificationTarget.Garage -> AppDestination.Garage.path
                            NotificationTarget.MealPlan -> AppDestination.MealPlan.path
                            is NotificationTarget.Workout -> AppDestination.Workout.withId(target.id)
                            is NotificationTarget.Vehicle -> AppDestination.Vehicle.withId(target.id)
                        }
                    if (nav.currentBackStackEntry == notifications) nav.navigateSingleTop(route)
                },
                backLabel = stringResource(R.string.pulse_app_back_to_home),
            )
        }
        debugGalleryDestination(currentAppearance, { updateTheme(it) }) { nav.popBackStack() }
    }
}
