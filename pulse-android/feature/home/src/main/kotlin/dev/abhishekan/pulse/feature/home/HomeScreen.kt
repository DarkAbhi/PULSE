package dev.abhishekan.pulse.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.domain.repository.DashboardPreferencesRepository
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.patterns.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.time.Instant
import java.time.ZoneId

@Composable
fun HomeRoute(
    username: String,
    onHorizon: () -> Unit,
    onFitness: () -> Unit,
    onNotifications: () -> Unit,
    onMissingProfile: () -> Unit = {},
    onProfile: (() -> Unit)? = null,
    onGarage: () -> Unit = {},
    onMealPlan: () -> Unit = {},
    onQuests: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
) {
    val currency by UserCurrency.changes.collectAsStateWithLifecycle()
    val dashboardCompact by koinInject<DashboardPreferencesRepository>().compact.collectAsStateWithLifecycle(false)
    val timeZone by UserTimeZone.changes.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val offerOnboarding by rememberUpdatedState(onMissingProfile)
    LaunchedEffect(viewModel, timeZone) {
        viewModel.onAction(HomeAction.TimeZoneChanged(timeZone))
    }
    LaunchedEffect(state.needsProfile) {
        if (state.needsProfile) offerOnboarding()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(HomeAction.Resumed)
    }
    HomeScreen(
        username = username,
        state = state,
        onAction = viewModel::onAction,
        onHorizon = onHorizon,
        onFitness = onFitness,
        onNotifications = onNotifications,
        now = now,
        onProfile = onProfile,
        timeZone = timeZone,
        currency = currency,
        onGarage = onGarage,
        onMealPlan = onMealPlan,
        onQuests = onQuests,
        dashboardCompact = dashboardCompact,
    )
}

@Composable
fun HomeScreen(
    username: String,
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onHorizon: () -> Unit,
    onFitness: () -> Unit,
    onNotifications: () -> Unit,
    now: Instant,
    onProfile: (() -> Unit)? = null,
    timeZone: ZoneId = UserTimeZone.zone,
    currency: String = UserCurrency.code,
    onGarage: () -> Unit = {},
    onMealPlan: () -> Unit = {},
    onQuests: () -> Unit = {},
    dashboardCompact: Boolean = false,
) {
    val colors = PulseTheme.colors
    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize().background(colors.background).testTag("dashboard-background")) {
        PulseAdaptiveLayout(Modifier.fillMaxSize()) { size ->
            if (state.isInitialLoading) {
                PulseCenteredLoadingState(
                    stringResource(R.string.pulse_home_loading_your_dashboard),
                    Modifier.fillMaxSize().padding(LocalPulseContentPadding.current),
                )
                return@PulseAdaptiveLayout
            }
            val compact = size == PulseWindowSize.Compact
            val stackCards = compact || LocalDensity.current.fontScale > 1.3f
            val margin = size.pagePadding
            val wideColumns = !stackCards && size == PulseWindowSize.Expanded && LocalConfiguration.current.screenWidthDp >= 1200
            val sectionGap = if (dashboardCompact) PulseTokens.medium else PulseTokens.cardPadding
            Column(
                Modifier
                    .widthIn(max = PulseTokens.contentWidth)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(LocalPulseContentPadding.current)
                    .padding(margin),
                verticalArrangement = Arrangement.spacedBy(sectionGap),
            ) {
                DashboardGreeting(
                    name = state.profileName ?: username,
                    compact = stackCards,
                    dense = dashboardCompact,
                    onNotifications = onNotifications,
                    onProfile = onProfile,
                    notificationCount = state.notificationCount,
                    now = now,
                    timeZone = timeZone,
                )
                val retryProfile = { onAction(HomeAction.Retry(HomeSection.Profile)) }
                when (val profile = state.profile.displayed) {
                    is Section.Error -> {
                        PulseText(profile.failure.message.asString())
                        PulseButton(stringResource(R.string.pulse_home_retry_profile), retryProfile, style = PulseButtonStyle.Text)
                    }
                    Section.Loading ->
                        PulseCenteredLoadingState(stringResource(R.string.pulse_home_loading_your_profile), Modifier.fillMaxWidth())
                    else -> Unit
                }
                state.profile.refreshFailure?.let { PulseErrorState(it.message, retryProfile) }
                val retry = { section: HomeSection -> onAction(HomeAction.Retry(section)) }
                // Web: one column on phones, a 2 x 2 grid of tiles and glance cards from tablet width.
                val columns = if (stackCards) 1 else 2
                DashboardQuickActions(
                    state,
                    if (dashboardCompact && wideColumns) 4 else columns,
                    currency,
                    timeZone,
                    retry,
                    onHorizon,
                    onFitness,
                    onGarage,
                    onMealPlan,
                    gap = sectionGap,
                )
                DashboardGlance(
                    state.fitnessToday,
                    if (wideColumns) 4 else columns,
                    { retry(HomeSection.FitnessToday) },
                    onFitness,
                    settings = (state.questSettings.displayed as? Section.Content)?.value,
                )
                DashboardQuestHeatmap(state.quests, { retry(HomeSection.Quests) }, onQuests)
                val recent = state.recentActivity.displayed
                DashboardRecentActivity(
                    entries =
                        when (recent) {
                            is Section.Content -> recent.value
                            Section.Empty -> emptyList()
                            else -> null
                        },
                    loading = recent == Section.Loading,
                    onRetry = { retry(HomeSection.RecentActivity) },
                )
                Spacer(Modifier.height(if (compact) 48.dp else 160.dp))
            }
        }
    }
}
