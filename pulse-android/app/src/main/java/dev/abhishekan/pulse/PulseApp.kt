package dev.abhishekan.pulse

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.NotificationTarget
import dev.abhishekan.pulse.core.domain.model.SessionState
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.ui.DraftSource
import dev.abhishekan.pulse.core.ui.LocalPulseNavigationHidingScreens
import dev.abhishekan.pulse.core.ui.PulseActivityTransitions
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseBottomNavigationBar
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseNavigationItem
import dev.abhishekan.pulse.core.ui.components.PulseNavigationRail
import dev.abhishekan.pulse.core.ui.components.PulseSnackbar
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarProvider
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.feature.auth.LoginRoute
import dev.abhishekan.pulse.feature.fitness.FitnessRoute
import dev.abhishekan.pulse.feature.fitness.WorkoutRoute
import dev.abhishekan.pulse.feature.garage.GarageRoute
import dev.abhishekan.pulse.feature.garage.VehicleDetailRoute
import dev.abhishekan.pulse.feature.home.HomeRoute
import dev.abhishekan.pulse.feature.horizon.HorizonRoute
import dev.abhishekan.pulse.feature.mealplan.MealPlanRoute
import dev.abhishekan.pulse.feature.notifications.NotificationsRoute
import dev.abhishekan.pulse.feature.profile.ProfileRoute
import dev.abhishekan.pulse.feature.profile.ProfileSetupPrompt
import org.koin.androidx.compose.koinViewModel
import dev.abhishekan.pulse.feature.profile.R as FeatureProfileR

@Composable
fun PulseApp(
    appearance: AppearanceUiState,
    onTheme: (ThemePreference) -> Unit,
    viewModel: SessionViewModel = koinViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.checkCurrentExpiry() }
    val state by viewModel.session.collectAsStateWithLifecycle()
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    val unsavedChanges by viewModel.unsavedChanges.collectAsStateWithLifecycle()
    when (val current = state) {
        SessionState.Checking ->
            Scaffold(containerColor = PulseTheme.colors.background, contentColor = PulseTheme.colors.textPrimary) { insets ->
                PulseAdaptiveLayout(Modifier.fillMaxSize().padding(insets)) { size ->
                    PulseCenteredLoadingState(
                        stringResource(R.string.pulse_app_checking_your_session),
                        Modifier.fillMaxSize().padding(size.pagePadding),
                    )
                }
            }
        is SessionState.Unavailable ->
            SessionUnavailable(
                current.failure,
                operation.running,
                viewModel::retry,
                viewModel::forgetSession,
            )
        is SessionState.SignedOut -> {
            val nav = rememberNavController()
            val notice =
                when {
                    current.storageProblem ->
                        stringResource(
                            R.string.pulse_app_you_re_signed_out_secure_storage_needs_attention_retry,
                        )
                    current.expired ->
                        stringResource(
                            R.string.pulse_app_your_session_has_expired_welcome_back_please_sign_in,
                        )
                    else -> null
                }
            NavHost(nav, startDestination = "login") { composable("login") { LoginRoute(notice) } }
        }
        is SessionState.Authenticated ->
            key(current.username) {
                SignedInApp(
                    current.username,
                    appearance,
                    onTheme,
                    operation,
                    viewModel::logOut,
                    viewModel::clearError,
                    unsavedChanges,
                )
            }
    }
}

@Composable
private fun SessionUnavailable(
    failure: Failure,
    busy: Boolean,
    onRetry: () -> Unit,
    onForget: () -> Unit,
) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    Scaffold(containerColor = PulseTheme.colors.background, contentColor = PulseTheme.colors.textPrimary) { insets ->
        PulseAdaptiveLayout(Modifier.fillMaxSize().padding(insets)) { size ->
            Box(Modifier.fillMaxSize().padding(size.pagePadding), contentAlignment = Alignment.Center) {
                PulseCard(
                    Modifier
                        .widthIn(
                            max = PulseTokens.readingWidth,
                        ).fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    PulseText(
                        stringResource(R.string.pulse_app_we_couldn_t_open_your_session),
                        style = PulseTextStyle.Title,
                    )
                    PulseText(
                        stringResource(
                            R.string.pulse_app_we_couldn_t_validate_your_session_1_s,
                            failure.message.asString(),
                        ),
                    )
                    PulseText(
                        stringResource(
                            R.string.pulse_app_your_saved_session_has_been_kept_on_this_device,
                        ),
                        color = PulseTheme.colors.textSecondary,
                    )
                    PulseButton(stringResource(R.string.pulse_app_try_again), onRetry, enabled = !busy)
                    PulseButton(stringResource(R.string.pulse_app_forget_this_device_s_session), {
                        confirm =
                            true
                    }, enabled = !busy, style = PulseButtonStyle.Text)
                }
            }
        }
    }
    if (confirm) {
        PulseDialog(
            stringResource(R.string.pulse_app_forget_this_session),
            stringResource(R.string.pulse_app_this_removes_this_device_s_saved_session_it_won),
            stringResource(R.string.pulse_app_forget_session),
            {
                confirm = false
                onForget()
            },
            { confirm = false },
        )
    }
}

@Composable
private fun SignedInApp(
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
    val hidingScreens = remember { mutableStateMapOf<Any, Unit>() }
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
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Some keyboards resize the root before IME insets arrive; preserve space for the form.
        val showBars = maxHeight >= PulseTokens.minimumNavigationHeight
        val showNavigation = destination != AppDestination.Gallery && showBars
        val navigationItems =
            AppDestination.tabs.map { tab ->
                PulseNavigationItem(stringResource(tab.label)) {
                    Icon(painterResource(checkNotNull(tab.icon)), null)
                }
            }
        val onNavigationSelected: (Int) -> Unit = { index ->
            val target = AppDestination.tabs[index]
            if (target != destination?.tab || destination == AppDestination.Notifications) selectDestination(target)
        }
        Row(Modifier.fillMaxSize().background(PulseTheme.colors.background)) {
            if (isTablet && showNavigation) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
                        .padding(
                            start = PulseTheme.spacing.cardPadding,
                            end = PulseTheme.spacing.medium,
                            top = PulseTheme.spacing.medium,
                            bottom = PulseTheme.spacing.medium,
                        ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    PulseNavigationRail(navigationItems, AppDestination.tabs.indexOf(destination?.tab), onNavigationSelected)
                }
            }
            Scaffold(
                containerColor = PulseTheme.colors.background,
                contentColor = PulseTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!isTablet && showNavigation && destination in AppDestination.tabs.take(5) && hidingScreens.isEmpty()) {
                        PulseBottomNavigationBar(
                            navigationItems.take(5),
                            AppDestination.tabs.indexOf(destination?.tab),
                            onNavigationSelected,
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) { PulseSnackbar(it) } },
            ) { insets ->
                CompositionLocalProvider(LocalPulseNavigationHidingScreens provides hidingScreens) {
                    PulseSnackbarProvider(snackbar) {
                        NavHost(
                            nav,
                            startDestination = AppDestination.Home.route,
                            modifier = Modifier.padding(insets).consumeWindowInsets(insets).clipToBounds(),
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
                                    { selectDestination(AppDestination.Horizon) },
                                    onFitness = { selectDestination(AppDestination.Fitness) },
                                    onNotifications = { nav.navigateSingleTop(AppDestination.Notifications.path) },
                                    onProfile =
                                        if (isTablet) {
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
                                        if (nav.currentBackStackEntry == vehicle) nav.popBackStack()
                                    },
                                    backLabel = backToNotifications,
                                )
                            }
                            composable(AppDestination.Horizon.route) {
                                HorizonRoute()
                            }
                            composable(AppDestination.Fitness.route) {
                                FitnessRoute(
                                    onWorkout = { id -> nav.navigateSingleTop(AppDestination.Workout.withId(id)) },
                                )
                            }
                            composable(AppDestination.Workout.route, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                                WorkoutRoute(
                                    onDeleted = { nav.popBackStack() },
                                    backLabel = backToNotifications ?: stringResource(R.string.pulse_app_back_to_fitness),
                                )
                            }
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
                                    appearance.theme,
                                    appearance.error,
                                    onTheme,
                                    operation.running,
                                    unsavedChanges.any { it != DraftSource.Profile },
                                    onLogout,
                                    profileEntry.arguments?.getBoolean("editName") == true,
                                    backLabel = if (isTablet) null else stringResource(R.string.pulse_app_back_to_home),
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
                            debugGalleryDestination { nav.popBackStack() }
                        }
                    }
                }
            }
        }
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
