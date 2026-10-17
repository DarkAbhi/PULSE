package dev.abhishekan.pulse

import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.asString

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.navArgument
import dev.abhishekan.pulse.feature.mealplan.MealPlanRoute
import dev.abhishekan.pulse.feature.garage.GarageRoute
import dev.abhishekan.pulse.feature.horizon.HorizonRoute
import dev.abhishekan.pulse.feature.fitness.FitnessRoute
import dev.abhishekan.pulse.feature.fitness.WorkoutRoute
import androidx.navigation.compose.*
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.SessionState
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.R as UiR
import dev.abhishekan.pulse.core.ui.components.*
import dev.abhishekan.pulse.feature.auth.LoginRoute
import dev.abhishekan.pulse.feature.home.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun PulseApp(appearance: AppearanceUiState, onTheme: (ThemePreference) -> Unit, viewModel: SessionViewModel = koinViewModel()) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.checkCurrentExpiry() }
    val state by viewModel.session.collectAsStateWithLifecycle()
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    when (val current = state) {
        SessionState.Checking -> Scaffold { insets ->
            Box(Modifier.fillMaxSize().padding(insets).padding(PulseTokens.cardPadding), contentAlignment = Alignment.Center) {
                PulseLoadingState(stringResource(R.string.pulse_app_checking_your_session))
            }
        }
        is SessionState.Unavailable -> SessionUnavailable(current.failure, operation.running, viewModel::retry, viewModel::forgetSession)
        is SessionState.SignedOut -> {
            val nav = rememberNavController()
            val notice = when {
                current.storageProblem -> stringResource(R.string.pulse_app_you_re_signed_out_secure_storage_needs_attention_retry)
                current.expired -> stringResource(R.string.pulse_app_your_session_has_expired_welcome_back_please_sign_in)
                else -> null
            }
            NavHost(nav, startDestination = "login") { composable("login") { LoginRoute(notice) } }
        }
        is SessionState.Authenticated -> key(current.username) {
            SignedInApp(current.username, appearance, onTheme, operation, viewModel::logOut, viewModel::clearError)
        }
    }
}

@Composable
private fun SessionUnavailable(failure: Failure, busy: Boolean, onRetry: () -> Unit, onForget: () -> Unit) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    Scaffold { insets ->
        Box(Modifier.fillMaxSize().padding(insets).padding(PulseTokens.cardPadding), contentAlignment = Alignment.Center) {
            PulseCard(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth().verticalScroll(rememberScrollState())) {
                PulseText(stringResource(R.string.pulse_app_we_couldn_t_open_your_session), style = PulseTextStyle.Title)
                PulseText(stringResource(R.string.pulse_app_we_couldn_t_validate_your_session_1_s , failure.message.asString()))
                PulseText(stringResource(R.string.pulse_app_your_saved_session_has_been_kept_on_this_device), color = MaterialTheme.colorScheme.onSurfaceVariant)
                PulseButton(stringResource(R.string.pulse_app_try_again), onRetry, enabled = !busy)
                PulseTextButton(stringResource(R.string.pulse_app_forget_this_device_s_session), { confirm = true }, enabled = !busy)
            }
        }
    }
    if (confirm) PulseDialog(stringResource(R.string.pulse_app_forget_this_session), stringResource(R.string.pulse_app_this_removes_this_device_s_saved_session_it_won), stringResource(R.string.pulse_app_forget_session),
        { confirm = false; onForget() }, { confirm = false })
}

@Composable
private fun SignedInApp(username: String, appearance: AppearanceUiState, onTheme: (ThemePreference) -> Unit,
    operation: SessionOperation, onLogout: () -> Unit, clearError: () -> Unit) {
    val nav = rememberNavController()
    var hasFitnessDraft by rememberSaveable { mutableStateOf(false) }
    var hasMealDraft by rememberSaveable { mutableStateOf(false) }
    var hasGarageDraft by rememberSaveable { mutableStateOf(false) }
    var hasHorizonDraft by rememberSaveable { mutableStateOf(false) }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val inSpaces = entry?.destination?.hierarchy?.any { it.route == "spaces" } == true
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val openFitness = {
        nav.navigate("spaces") {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        if (nav.currentDestination?.route in listOf("space-list", "horizon", "garage", "mealplan")) nav.navigate("fitness") { launchSingleTop = true }
    }
    val openHorizon = {
        nav.navigate("spaces") {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true; restoreState = true
        }
        if (nav.currentDestination?.route != "horizon") nav.navigate("horizon") { launchSingleTop = true }
    }
    val openGarage = {
        nav.navigate("spaces") {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true; restoreState = true
        }
        if (nav.currentDestination?.route != "garage") nav.navigate("garage") { launchSingleTop = true }
    }
    val openMealPlan = {
        nav.navigate("spaces") {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true; restoreState = true
        }
        if (nav.currentDestination?.route != "mealplan") nav.navigate("mealplan") { launchSingleTop = true }
    }
    val snackbar = remember { SnackbarHostState() }
    val operationMessage = operation.error?.asString()
    LaunchedEffect(operationMessage) {
        operationMessage?.let { snackbar.showSnackbar(it); clearError() }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Some keyboards resize the root before IME insets arrive; preserve space for the form.
        val showBars = maxHeight >= PulseTokens.minimumNavigationHeight
        Scaffold(
            contentWindowInsets = if (showBars) ScaffoldDefaults.contentWindowInsets else WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
            topBar = {
                if (route != "gallery" && showBars) PulseTopAppBar(
                    when (route) { "you" -> stringResource(R.string.pulse_app_you); "notifications" -> stringResource(R.string.pulse_app_notifications); "space-list" -> stringResource(R.string.pulse_app_spaces); "fitness" -> stringResource(R.string.pulse_app_fitness); "workout/{id}" -> stringResource(R.string.pulse_app_workout); "horizon" -> stringResource(R.string.pulse_app_financial_horizon); "garage" -> stringResource(R.string.pulse_app_garage); "mealplan" -> stringResource(R.string.pulse_app_meal_plan); else -> stringResource(R.string.pulse_app_pulse) },
                    actions = {
                        if (route == "home") PulseIconButton(stringResource(R.string.pulse_app_notifications), { nav.navigate("notifications") { launchSingleTop = true } }) {
                            Icon(painterResource(UiR.drawable.pulse_bell), contentDescription = null)
                        }
                    },
                    navigationIcon = {
                        if (route == "notifications" || route == "fitness" || route == "horizon" || route == "garage" || route == "mealplan" || route == "workout/{id}") PulseIconButton(
                            when (route) { "notifications" -> stringResource(R.string.pulse_app_back_to_home); "fitness", "horizon", "garage", "mealplan" -> stringResource(R.string.pulse_app_back_to_spaces); else -> stringResource(R.string.pulse_app_back_to_fitness) },
                            { backDispatcher?.onBackPressed() }) {
                            Icon(painterResource(UiR.drawable.pulse_back), contentDescription = null)
                        }
                    },
                )
            },
            bottomBar = {
                if (route != "gallery" && showBars) PulseBottomNavigationBar(
                    listOf(PulseNavigationItem(stringResource(R.string.pulse_app_home)) { Icon(painterResource(UiR.drawable.pulse_home), null) },
                        PulseNavigationItem(stringResource(R.string.pulse_app_spaces)) { Icon(painterResource(UiR.drawable.pulse_spaces), null) },
                        PulseNavigationItem(stringResource(R.string.pulse_app_you)) { Icon(painterResource(UiR.drawable.pulse_person), null) }),
                    when { route == "you" -> 2; inSpaces -> 1; else -> 0 },
                    { index -> nav.navigate(listOf("home", "spaces", "you")[index]) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    } },
                )
            },
            snackbarHost = { SnackbarHost(snackbar) { PulseSnackbar(it) } },
        ) { insets ->
            NavHost(nav, startDestination = "home", modifier = Modifier.padding(insets).consumeWindowInsets(insets)) {
                composable("home") { HomeRoute(username, { nav.navigate("notifications") { launchSingleTop = true } }, openFitness, openHorizon, openGarage, openMealPlan) }
                navigation(startDestination = "space-list", route = "spaces") {
                    composable("space-list") { SpacesScreen({ nav.navigate("fitness") { launchSingleTop = true } }, { nav.navigate("horizon") { launchSingleTop = true } }, { nav.navigate("garage") { launchSingleTop = true } }, { nav.navigate("mealplan") { launchSingleTop = true } }) }
                    composable("mealplan") { MealPlanRoute(onDraftChanged = { hasMealDraft = it }) }
                    composable("garage") { GarageRoute(onDraftChanged = { hasGarageDraft = it }) }
                    composable("horizon") { HorizonRoute(onDraftChanged = { hasHorizonDraft = it }) }
                    composable("fitness") { FitnessRoute({ id -> nav.navigate("workout/$id") { launchSingleTop = true } }) }
                    composable("workout/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                        WorkoutRoute(onDeleted = { nav.popBackStack() }, onDraftChanged = { hasFitnessDraft = it })
                    }
                }
                composable("you") {
                    YouScreen(username, appearance, onTheme, operation.running, hasFitnessDraft || hasHorizonDraft || hasGarageDraft || hasMealDraft, onLogout, { nav.navigate("gallery") })
                }
                composable("notifications") {
                    val homeEntry = remember(it) { nav.getBackStackEntry("home") }
                    val home: HomeViewModel = koinViewModel(viewModelStoreOwner = homeEntry)
                    val state by home.uiState.collectAsStateWithLifecycle()
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PulseTokens.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                        PulseCard(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth()) {
                            PulseText(stringResource(R.string.pulse_app_your_latest_updates), style = PulseTextStyle.Title)
                            NotificationSection(state.notifications, home::retryNotifications)
                        }
                    }
                }
                debugGalleryDestination()
            }
        }
    }
}

@Composable
private fun YouScreen(username: String, appearance: AppearanceUiState, onTheme: (ThemePreference) -> Unit,
    loggingOut: Boolean, hasFitnessDraft: Boolean, onLogout: () -> Unit, onGallery: () -> Unit) {
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth().verticalScroll(rememberScrollState()).padding(PulseTokens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
            PulseCard(Modifier.fillMaxWidth()) {
                PulseText(username, style = PulseTextStyle.Title)
                PulseText(stringResource(R.string.pulse_app_your_private_everyday_hub))
            }
            PulseCard(Modifier.fillMaxWidth()) {
                PulseText(stringResource(R.string.pulse_app_appearance), style = PulseTextStyle.Title)
                Column(Modifier.selectableGroup()) {
                    ThemePreference.entries.forEach { theme ->
                        PulseRadioButton(appearance.theme == theme, { onTheme(theme) }, theme.label, Modifier.fillMaxWidth())
                    }
                }
                appearance.error?.let { PulseText(it, color = MaterialTheme.colorScheme.error) }
            }
            PulseButton(stringResource(R.string.pulse_app_log_out), { if (hasFitnessDraft) confirmLogout = true else onLogout() }, Modifier.fillMaxWidth(), loading = loggingOut)
            DebugGalleryAction(onGallery)
        }
    }
    if (confirmLogout) PulseDialog(stringResource(R.string.pulse_app_log_out_and_discard_edits),
        stringResource(R.string.pulse_app_you_have_unsaved_changes_logging_out_will_discard_this),
        stringResource(R.string.pulse_app_log_out), { confirmLogout = false; onLogout() }, { confirmLogout = false })
}

@Composable
private fun SpacesScreen(onFitness: () -> Unit, onHorizon: () -> Unit, onGarage: () -> Unit, onMealPlan: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth().verticalScroll(rememberScrollState()).padding(PulseTokens.medium),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
            PulseText(stringResource(R.string.pulse_app_make_room_for_your_everyday), style = PulseTextStyle.Display)
            PulseCard(Modifier.fillMaxWidth()) {
                PulseText(stringResource(R.string.pulse_app_fitness), style = PulseTextStyle.Title)
                PulseText(stringResource(R.string.pulse_app_find_your_rhythm_record_workouts_revisit_your_activity_and))
                PulseButton(stringResource(R.string.pulse_app_open_fitness), onFitness, Modifier.fillMaxWidth())
            }
            PulseCard(Modifier.fillMaxWidth()) {
                PulseText(stringResource(R.string.pulse_app_financial_horizon), style = PulseTextStyle.Title)
                PulseText(stringResource(R.string.pulse_app_your_room_to_move_review_commitments_track_transactions_and))
                PulseButton(stringResource(R.string.pulse_app_open_financial_horizon), onHorizon, Modifier.fillMaxWidth())
            }
            PulseCard(Modifier.fillMaxWidth()) {
                PulseText(stringResource(R.string.pulse_app_garage), style = PulseTextStyle.Title)
                PulseText(stringResource(R.string.pulse_app_care_for_every_journey_track_fuel_tire_pressure_maintenance))
                PulseButton(stringResource(R.string.pulse_app_open_garage), onGarage, Modifier.fillMaxWidth())
            }
            PulseCard(Modifier.fillMaxWidth()) {
                PulseText(stringResource(R.string.pulse_app_meal_plan), style = PulseTextStyle.Title)
                PulseText(stringResource(R.string.pulse_app_a_little_care_for_your_day_plan_the_week))
                PulseButton(stringResource(R.string.pulse_app_open_meal_plan), onMealPlan, Modifier.fillMaxWidth())
            }
        }
    }
}
