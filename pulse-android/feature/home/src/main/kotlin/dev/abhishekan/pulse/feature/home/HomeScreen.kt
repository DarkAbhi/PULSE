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
import androidx.compose.runtime.getValue
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
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import org.koin.androidx.compose.koinViewModel

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
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = (state.profile as? Section.Content)?.value
    androidx.compose.runtime.LaunchedEffect(profile) { if (profile?.hasProfile == false) onMissingProfile() }
    // The backend computes today in Asia/Kolkata; refresh after returning to the app.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.retryWorkout() }
    HomeScreen(
        username,
        state,
        viewModel::retryProfile,
        viewModel::retryWorkout,
        viewModel::retryHorizon,
        onHorizon,
        onFitness,
        onNotifications,
        onProfile,
    )
}

@Composable
fun HomeScreen(
    username: String,
    state: HomeUiState,
    retryProfile: () -> Unit,
    retryWorkout: () -> Unit,
    retryHorizon: () -> Unit,
    onHorizon: () -> Unit,
    onFitness: () -> Unit,
    onNotifications: () -> Unit,
    onProfile: (() -> Unit)? = null,
) {
    val colors = PulseTheme.colors
    Box(Modifier.fillMaxSize().background(colors.background).testTag("dashboard-background")) {
        PulseAdaptiveLayout(Modifier.fillMaxSize()) { size ->
            val compact = size == PulseWindowSize.Compact
            val stackCards = compact || LocalDensity.current.fontScale > 1.3f
            val margin = size.pagePadding
            Column(
                Modifier
                    .widthIn(max = PulseTokens.contentWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(LocalPulseContentPadding.current)
                    .padding(margin),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
            ) {
                DashboardGreeting(
                    name = (state.profile as? Section.Content)?.value?.name ?: username,
                    compact = stackCards,
                    onNotifications = onNotifications,
                    onProfile = onProfile,
                )
                when (val profile = state.profile) {
                    is Section.Error -> {
                        PulseText(profile.failure.message.asString())
                        PulseTextButton(stringResource(R.string.pulse_home_retry_profile), retryProfile)
                    }
                    Section.Loading -> PulseLoadingState(stringResource(R.string.pulse_home_loading_your_profile))
                    else -> Unit
                }
                DashboardCards(stackCards) {
                    HorizonCard(state.horizon, retryHorizon, onHorizon)
                    WorkoutCard(state.workout, retryWorkout, onFitness)
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
