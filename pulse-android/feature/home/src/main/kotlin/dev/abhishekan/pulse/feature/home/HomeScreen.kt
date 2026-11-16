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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import org.koin.androidx.compose.koinViewModel
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
    viewModel: HomeViewModel = koinViewModel(),
) {
    val currency by UserCurrency.changes.collectAsStateWithLifecycle()
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
            Column(
                Modifier
                    .widthIn(max = PulseTokens.contentWidth)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(LocalPulseContentPadding.current)
                    .padding(margin),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
            ) {
                DashboardGreeting(
                    name = state.profileName ?: username,
                    compact = stackCards,
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
                DashboardCards(stackCards) {
                    HorizonCard(state.horizon, { onAction(HomeAction.Retry(HomeSection.Horizon)) }, onHorizon, currency)
                    WorkoutCard(state.workout, { onAction(HomeAction.Retry(HomeSection.Workout)) }, onFitness)
                }
                Spacer(Modifier.height(if (compact) 48.dp else 160.dp))
            }
        }
    }
}

@Composable
private fun DashboardCards(
    stacked: Boolean,
    content: @Composable () -> Unit,
) {
    Layout(content, Modifier.fillMaxWidth()) { cards, constraints ->
        val gap = PulseTokens.cardPadding.roundToPx()
        val width = constraints.maxWidth
        val cardWidth = if (stacked) width else (width - gap) / 2
        // Size both cards to the tallest content, including wrapped text and retry states.
        val cardHeight = cards.maxOf { it.maxIntrinsicHeight(cardWidth) }
        val placeables = cards.map { it.measure(Constraints.fixed(cardWidth, cardHeight)) }
        layout(width, if (stacked) cardHeight * 2 + gap else cardHeight) {
            placeables.forEachIndexed { index, card ->
                card.placeRelative(
                    x = if (stacked) 0 else index * (cardWidth + gap),
                    y = if (stacked) index * (cardHeight + gap) else 0,
                )
            }
        }
    }
}
