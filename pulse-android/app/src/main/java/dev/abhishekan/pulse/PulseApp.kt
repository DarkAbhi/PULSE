package dev.abhishekan.pulse
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.NotificationTarget
import dev.abhishekan.pulse.core.domain.model.SessionState
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseBottomNavigationBar
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseIconButton
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseNavigationItem
import dev.abhishekan.pulse.core.ui.components.PulseNavigationRail
import dev.abhishekan.pulse.core.ui.components.PulseSnackbar
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTopAppBar
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.feature.auth.LoginRoute
import dev.abhishekan.pulse.feature.fitness.FitnessRoute
import dev.abhishekan.pulse.feature.fitness.WorkoutRoute
import dev.abhishekan.pulse.feature.garage.GarageRoute
import dev.abhishekan.pulse.feature.home.HomeRoute
import dev.abhishekan.pulse.feature.horizon.HorizonRoute
import dev.abhishekan.pulse.feature.mealplan.MealPlanRoute
import dev.abhishekan.pulse.feature.notifications.NotificationsRoute
import dev.abhishekan.pulse.feature.profile.ProfileRoute
import dev.abhishekan.pulse.feature.profile.ProfileSetupPrompt
import dev.abhishekan.pulse.feature.profile.ProfileViewModel
import org.koin.androidx.compose.koinViewModel
import dev.abhishekan.pulse.core.ui.R as UiR

@Composable
fun PulseApp(
    appearance: AppearanceUiState,
    onTheme: (ThemePreference) -> Unit,
    viewModel: SessionViewModel = koinViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.checkCurrentExpiry() }
    val state by viewModel.session.collectAsStateWithLifecycle()
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    when (val current = state) {
        SessionState.Checking ->
            Scaffold(containerColor = PulseTheme.colors.background, contentColor = PulseTheme.colors.textPrimary) { insets ->
                Box(
                    Modifier.fillMaxSize().padding(insets).padding(PulseTokens.cardPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    PulseLoadingState(stringResource(R.string.pulse_app_checking_your_session))
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
        Box(
            Modifier.fillMaxSize().padding(insets).padding(PulseTokens.cardPadding),
            contentAlignment = Alignment.Center,
        ) {
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
                PulseTextButton(stringResource(R.string.pulse_app_forget_this_device_s_session), {
                    confirm =
                        true
                }, enabled = !busy)
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
) {
    val nav = rememberNavController()
    var onboardingOffered by rememberSaveable { mutableStateOf(false) }
    var onboardingVisible by rememberSaveable { mutableStateOf(false) }
    var editProfileName by rememberSaveable { mutableStateOf(false) }
    var hasPasswordEdits by rememberSaveable { mutableStateOf(false) }
    var pendingDestination by remember { mutableStateOf<String?>(null) }
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    var hasFitnessDraft by rememberSaveable { mutableStateOf(false) }
    var hasMealDraft by rememberSaveable { mutableStateOf(false) }
    var hasGarageDraft by rememberSaveable { mutableStateOf(false) }
    var hasHorizonDraft by rememberSaveable { mutableStateOf(false) }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val inSpaces = entry?.destination?.hierarchy?.any { it.route == "spaces" } == true
    val navigateTo: (String) -> Unit = { destination ->
        nav.navigate(destination) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = !isTablet || destination != "home"
        }
    }
    val selectDestination: (String) -> Unit = { destination ->
        if (hasPasswordEdits && route == "you" && destination != "you") {
            pendingDestination = destination
        } else {
            navigateTo(destination)
        }
    }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val openFitness = {
        nav.navigate("spaces") {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        if (nav.currentDestination?.route in
            listOf("space-list", "horizon", "garage", "mealplan")
        ) {
            nav.navigate("fitness") {
                launchSingleTop =
                    true
            }
        }
    }
    val openHorizon = {
        nav.navigate("spaces") {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        if (nav.currentDestination?.route !=
            "horizon"
        ) {
            nav.navigate("horizon") { launchSingleTop = true }
        }
    }
    val openGarage = {
        nav.navigate("spaces") {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        if (nav.currentDestination?.route !=
            "garage"
        ) {
            nav.navigate("garage") { launchSingleTop = true }
        }
    }
    val openMealPlan = {
        nav.navigate("spaces") {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        if (nav.currentDestination?.route !=
            "mealplan"
        ) {
            nav.navigate("mealplan") { launchSingleTop = true }
        }
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
        val isTabletTab = isTablet && route in listOf("fitness", "horizon", "garage", "mealplan", "you")
        Row(Modifier.fillMaxSize()) {
            if (isTablet && route != "gallery" && showBars) {
                val destinations = listOf("home", "fitness", "horizon", "garage", "mealplan", "you")
                val selectedDestination =
                    when (route) {
                        "workout/{id}" -> "fitness"
                        "vehicle/{id}" -> "garage"
                        "notifications" -> "home"
                        else -> route
                    }
                PulseNavigationRail(
                    listOf(
                        PulseNavigationItem(stringResource(R.string.pulse_app_home)) {
                            Icon(painterResource(UiR.drawable.pulse_home), null)
                        },
                        PulseNavigationItem(stringResource(R.string.pulse_app_fitness)) {
                            Icon(painterResource(UiR.drawable.pulse_fitness), null)
                        },
                        PulseNavigationItem(stringResource(R.string.pulse_app_horizon)) {
                            Icon(painterResource(UiR.drawable.pulse_horizon), null)
                        },
                        PulseNavigationItem(stringResource(R.string.pulse_app_garage)) {
                            Icon(painterResource(UiR.drawable.pulse_garage), null)
                        },
                        PulseNavigationItem(stringResource(R.string.pulse_app_meals)) {
                            Icon(painterResource(UiR.drawable.pulse_meals), null)
                        },
                        PulseNavigationItem(stringResource(R.string.pulse_app_you)) {
                            Icon(painterResource(UiR.drawable.pulse_person), null)
                        },
                    ),
                    destinations.indexOf(selectedDestination),
                    { index ->
                        if (destinations[index] != selectedDestination || route == "notifications") {
                            selectDestination(destinations[index])
                        }
                    },
                )
            }
            Scaffold(
                containerColor = PulseTheme.colors.background,
                contentColor = PulseTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
                contentWindowInsets =
                    if (route == "home") {
                        ScaffoldDefaults.contentWindowInsets.only(
                            if (isTablet) WindowInsetsSides.Horizontal else WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                        )
                    } else if (showBars) {
                        ScaffoldDefaults.contentWindowInsets
                    } else {
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal,
                        )
                    },
                topBar = {
                    if (route != "gallery" &&
                        route != "home" &&
                        showBars &&
                        (!isTabletTab || nav.previousBackStackEntry?.destination?.route == "notifications")
                    ) {
                        PulseTopAppBar(
                            if (isTabletTab) {
                                ""
                            } else {
                                when (route) {
                                    "you" -> stringResource(R.string.pulse_app_you)
                                    "notifications" -> stringResource(R.string.pulse_app_notifications)
                                    "space-list" -> stringResource(R.string.pulse_app_spaces)
                                    "fitness" -> stringResource(R.string.pulse_app_fitness)
                                    "workout/{id}" -> stringResource(R.string.pulse_app_workout)
                                    "horizon" -> stringResource(R.string.pulse_app_financial_horizon)
                                    "garage", "vehicle/{id}" -> stringResource(R.string.pulse_app_garage)
                                    "mealplan" -> stringResource(R.string.pulse_app_meal_plan)
                                    else -> stringResource(R.string.pulse_app_pulse)
                                }
                            },
                            navigationIcon = {
                                if (route == "notifications" ||
                                    nav.previousBackStackEntry?.destination?.route == "notifications" ||
                                    (!isTablet && route in listOf("fitness", "horizon", "garage", "mealplan")) ||
                                    route == "vehicle/{id}" ||
                                    route == "workout/{id}"
                                ) {
                                    PulseIconButton(
                                        if (nav.previousBackStackEntry?.destination?.route == "notifications") {
                                            stringResource(R.string.pulse_app_back_to_notifications)
                                        } else {
                                            when (route) {
                                                "notifications" ->
                                                    stringResource(
                                                        R.string.pulse_app_back_to_home,
                                                    )
                                                "vehicle/{id}" ->
                                                    if (isTablet) {
                                                        stringResource(R.string.pulse_app_back_to_garage)
                                                    } else {
                                                        stringResource(R.string.pulse_app_back_to_spaces)
                                                    }
                                                "fitness", "horizon", "garage", "mealplan" ->
                                                    stringResource(
                                                        R.string.pulse_app_back_to_spaces,
                                                    )
                                                else -> stringResource(R.string.pulse_app_back_to_fitness)
                                            }
                                        },
                                        { backDispatcher?.onBackPressed() },
                                    ) {
                                        Icon(
                                            painterResource(UiR.drawable.pulse_back),
                                            contentDescription = null,
                                        )
                                    }
                                }
                            },
                        )
                    }
                },
                bottomBar = {
                    if (!isTablet && route != "gallery" && showBars) {
                        PulseBottomNavigationBar(
                            listOf(
                                PulseNavigationItem(stringResource(R.string.pulse_app_home)) {
                                    Icon(painterResource(UiR.drawable.pulse_home), null)
                                },
                                PulseNavigationItem(stringResource(R.string.pulse_app_spaces)) {
                                    Icon(painterResource(UiR.drawable.pulse_spaces), null)
                                },
                                PulseNavigationItem(stringResource(R.string.pulse_app_you)) {
                                    Icon(painterResource(UiR.drawable.pulse_person), null)
                                },
                            ),
                            when {
                                route == "you" -> 2
                                inSpaces -> 1
                                else -> 0
                            },
                            { index -> selectDestination(listOf("home", "spaces", "you")[index]) },
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) { PulseSnackbar(it) } },
            ) { insets ->
                NavHost(
                    nav,
                    startDestination = "home",
                    modifier = Modifier.padding(insets).consumeWindowInsets(insets),
                ) {
                    composable("home") {
                        HomeRoute(
                            username,
                            if (isTablet) ({ selectDestination("horizon") }) else openHorizon,
                            onFitness = if (isTablet) ({ selectDestination("fitness") }) else openFitness,
                            onNotifications = { nav.navigate("notifications") { launchSingleTop = true } },
                            onMissingProfile = {
                                if (!onboardingOffered) {
                                    onboardingOffered = true
                                    onboardingVisible = true
                                }
                            },
                        )
                        if (onboardingVisible) {
                            ProfileSetupPrompt({
                                onboardingVisible = false
                                editProfileName = true
                                nav.navigate("you") {
                                    launchSingleTop =
                                        true
                                }
                            }, { onboardingVisible = false })
                        }
                    }
                    navigation(startDestination = "space-list", route = "spaces") {
                        composable("space-list") {
                            SpacesScreen({
                                nav.navigate("fitness") {
                                    launchSingleTop =
                                        true
                                }
                            }, { nav.navigate("horizon") { launchSingleTop = true } }, {
                                nav.navigate("garage") {
                                    launchSingleTop =
                                        true
                                }
                            }, { nav.navigate("mealplan") { launchSingleTop = true } })
                        }
                        composable("mealplan") { MealPlanRoute(onDraftChanged = { hasMealDraft = it }) }
                        composable("garage") { GarageRoute(onDraftChanged = { hasGarageDraft = it }) }
                        composable("vehicle/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { destination ->
                            GarageRoute(onDraftChanged = { hasGarageDraft = it }, initialVehicle = destination.arguments?.getLong("id"))
                        }
                        composable("horizon") {
                            HorizonRoute(onDraftChanged = { hasHorizonDraft = it })
                        }
                        composable("fitness") {
                            FitnessRoute({ id ->
                                nav.navigate("workout/$id") {
                                    launchSingleTop =
                                        true
                                }
                            })
                        }
                        composable(
                            "workout/{id}",
                            arguments =
                                listOf(
                                    navArgument("id") {
                                        type =
                                            NavType.LongType
                                    },
                                ),
                        ) {
                            WorkoutRoute(onDeleted = { nav.popBackStack() }, onDraftChanged = {
                                hasFitnessDraft =
                                    it
                            })
                        }
                    }
                    composable("you") {
                        val profile: ProfileViewModel = koinViewModel()
                        ProfileRoute(
                            username,
                            appearance.theme,
                            appearance.error,
                            onTheme,
                            operation.running,
                            hasFitnessDraft || hasHorizonDraft || hasGarageDraft || hasMealDraft,
                            onLogout,
                            editProfileName,
                            { editProfileName = false },
                            profile,
                            onSensitiveEditsChanged = { hasPasswordEdits = it },
                        ) { DebugGalleryAction { nav.navigate("gallery") } }
                    }
                    composable("notifications") {
                        NotificationsRoute(onOpen = { target ->
                            // Navigate directly to the owning graph child, retaining Notifications underneath.
                            when (target) {
                                NotificationTarget.Fitness -> nav.navigate("fitness") { launchSingleTop = true }
                                NotificationTarget.Horizon -> nav.navigate("horizon") { launchSingleTop = true }
                                NotificationTarget.Garage -> nav.navigate("garage") { launchSingleTop = true }
                                NotificationTarget.MealPlan -> nav.navigate("mealplan") { launchSingleTop = true }
                                is NotificationTarget.Workout -> nav.navigate("workout/${target.id}") { launchSingleTop = true }
                                is NotificationTarget.Vehicle -> nav.navigate("vehicle/${target.id}") { launchSingleTop = true }
                            }
                        })
                    }
                    debugGalleryDestination()
                }
            }
        }
    }
    pendingDestination?.let { destination ->
        PulseDialog(
            stringResource(dev.abhishekan.pulse.feature.profile.R.string.pulse_profile_discard_title),
            stringResource(dev.abhishekan.pulse.feature.profile.R.string.pulse_profile_discard_body),
            stringResource(dev.abhishekan.pulse.feature.profile.R.string.pulse_profile_discard),
            {
                pendingDestination = null
                hasPasswordEdits = false
                navigateTo(destination)
            },
            { pendingDestination = null },
        )
    }
}

@Composable
private fun SpacesScreen(
    onFitness: () -> Unit,
    onHorizon: () -> Unit,
    onGarage: () -> Unit,
    onMealPlan: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(
                    max = PulseTokens.readingWidth,
                ).fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(PulseTokens.medium),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
        ) {
            PulseText(
                stringResource(R.string.pulse_app_make_room_for_your_everyday),
                style = PulseTextStyle.Display,
            )
            PulseCard(Modifier.fillMaxWidth()) {
                PulseText(stringResource(R.string.pulse_app_fitness), style = PulseTextStyle.Title)
                PulseText(
                    stringResource(
                        R.string.pulse_app_find_your_rhythm_record_workouts_revisit_your_activity_and,
                    ),
                )
                PulseButton(
                    stringResource(R.string.pulse_app_open_fitness),
                    onFitness,
                    Modifier.fillMaxWidth(),
                )
            }
            PulseCard(Modifier.fillMaxWidth()) {
                PulseText(
                    stringResource(R.string.pulse_app_financial_horizon),
                    style = PulseTextStyle.Title,
                )
                PulseText(
                    stringResource(
                        R.string.pulse_app_your_room_to_move_review_commitments_track_transactions_and,
                    ),
                )
                PulseButton(
                    stringResource(R.string.pulse_app_open_financial_horizon),
                    onHorizon,
                    Modifier.fillMaxWidth(),
                )
            }
            PulseCard(Modifier.fillMaxWidth()) {
                PulseText(stringResource(R.string.pulse_app_garage), style = PulseTextStyle.Title)
                PulseText(
                    stringResource(
                        R.string.pulse_app_care_for_every_journey_track_fuel_tire_pressure_maintenance,
                    ),
                )
                PulseButton(
                    stringResource(R.string.pulse_app_open_garage),
                    onGarage,
                    Modifier.fillMaxWidth(),
                )
            }
            PulseCard(Modifier.fillMaxWidth()) {
                PulseText(
                    stringResource(R.string.pulse_app_meal_plan),
                    style = PulseTextStyle.Title,
                )
                PulseText(
                    stringResource(R.string.pulse_app_a_little_care_for_your_day_plan_the_week),
                )
                PulseButton(
                    stringResource(R.string.pulse_app_open_meal_plan),
                    onMealPlan,
                    Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
