package dev.abhishekan.pulse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import dev.abhishekan.pulse.core.domain.*
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*
import dev.abhishekan.pulse.core.ui.R as UiR
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
                PulseLoadingState("Checking your session")
            }
        }
        is SessionState.Unavailable -> SessionUnavailable(current.failure, operation.running, viewModel::retry, viewModel::forgetSession)
        is SessionState.SignedOut -> {
            val nav = rememberNavController()
            val notice = when {
                current.storageProblem -> "You’re signed out. Secure storage needs attention; retry signing in or restart Pulse."
                current.expired -> "Your session has expired. Welcome back — please sign in again."
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
                PulseText("We couldn’t open your session", style = PulseTextStyle.Title)
                PulseText("We couldn’t validate your session. ${failure.message}")
                PulseText("Your saved session has been kept on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                PulseButton("Try again", onRetry, enabled = !busy)
                PulseTextButton("Forget this device’s session", { confirm = true }, enabled = !busy)
            }
        }
    }
    if (confirm) PulseDialog("Forget this session?", "This removes this device’s saved session. It won’t sign out your other devices.", "Forget session",
        { confirm = false; onForget() }, { confirm = false })
}

@Composable
private fun SignedInApp(username: String, appearance: AppearanceUiState, onTheme: (ThemePreference) -> Unit,
    operation: SessionOperation, onLogout: () -> Unit, clearError: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(operation.error) {
        operation.error?.let { snackbar.showSnackbar(it); clearError() }
    }
    Scaffold(
        topBar = {
            if (route != "gallery") PulseTopAppBar(
                when (route) { "you" -> "You"; "notifications" -> "Notifications"; else -> "Pulse" },
                actions = {
                    if (route == "home") PulseIconButton("Notifications", { nav.navigate("notifications") { launchSingleTop = true } }) {
                        Icon(painterResource(UiR.drawable.pulse_bell), contentDescription = null)
                    }
                },
                navigationIcon = {
                    if (route == "notifications") PulseIconButton("Back to Home", { nav.popBackStack() }) {
                        Icon(painterResource(UiR.drawable.pulse_back), contentDescription = null)
                    }
                },
            )
        },
        bottomBar = {
            if (route != "gallery") PulseBottomNavigationBar(
                listOf(PulseNavigationItem("Home") { Icon(painterResource(UiR.drawable.pulse_home), null) },
                    PulseNavigationItem("You") { Icon(painterResource(UiR.drawable.pulse_person), null) }),
                if (route == "you") 1 else 0,
                { index -> nav.navigate(if (index == 0) "home" else "you") {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) { PulseSnackbar(it) } },
    ) { insets ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(insets)) {
            composable("home") { HomeRoute(username, { nav.navigate("notifications") { launchSingleTop = true } }) }
            composable("you") {
                YouScreen(username, appearance, onTheme, operation.running, onLogout, { nav.navigate("gallery") })
            }
            composable("notifications") {
                val homeEntry = remember(it) { nav.getBackStackEntry("home") }
                val home: HomeViewModel = koinViewModel(viewModelStoreOwner = homeEntry)
                val state by home.uiState.collectAsStateWithLifecycle()
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PulseTokens.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                    PulseCard(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth()) {
                        PulseText("Your latest updates", style = PulseTextStyle.Title)
                        NotificationSection(state.notifications, home::retryNotifications)
                    }
                }
            }
            debugGalleryDestination()
        }
    }
}

@Composable
private fun YouScreen(username: String, appearance: AppearanceUiState, onTheme: (ThemePreference) -> Unit,
    loggingOut: Boolean, onLogout: () -> Unit, onGallery: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth().verticalScroll(rememberScrollState()).padding(PulseTokens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
            PulseCard(Modifier.fillMaxWidth()) {
                PulseText(username, style = PulseTextStyle.Title)
                PulseText("Your private everyday hub.")
            }
            PulseCard(Modifier.fillMaxWidth()) {
                PulseText("Appearance", style = PulseTextStyle.Title)
                Column(Modifier.selectableGroup()) {
                    ThemePreference.entries.forEach { theme ->
                        PulseRadioButton(appearance.theme == theme, { onTheme(theme) }, theme.name, Modifier.fillMaxWidth())
                    }
                }
                appearance.error?.let { PulseText(it, color = MaterialTheme.colorScheme.error) }
            }
            PulseButton("Log out", onLogout, Modifier.fillMaxWidth(), loading = loggingOut)
            DebugGalleryAction(onGallery)
        }
    }
}
