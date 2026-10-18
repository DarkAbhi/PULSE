package dev.abhishekan.pulse.feature.home
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseLightColors
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseIconButton
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import dev.abhishekan.pulse.core.ui.R as UiR

@Composable
fun HomeRoute(
    username: String,
    onHorizon: () -> Unit,
    onFitness: () -> Unit,
    onNotifications: () -> Unit,
    onMissingProfile: () -> Unit = {},
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
) {
    val colors = PulseTheme.colors
    val light = colors == PulseLightColors
    Box(Modifier.fillMaxSize().background(colors.background)) {
        Image(
            painterResource(R.drawable.dashboard_room),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().testTag("dashboard-background"),
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomEnd,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        colors.background.copy(alpha = if (light) 0.94f else 0.96f),
                        colors.background.copy(alpha = if (light) 0.72f else 0.88f),
                        colors.background.copy(alpha = if (light) 0.08f else 0.72f),
                    ),
                ),
            ),
        )
        PulseAdaptiveLayout(Modifier.fillMaxSize()) { size ->
            val compact = size == PulseWindowSize.Compact
            val stackCards = compact || LocalDensity.current.fontScale > 1.3f
            val margin = if (compact) PulseTokens.cardPadding else PulseTokens.large
            Column(
                Modifier
                    .widthIn(max = PulseTokens.contentWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(LocalPulseContentPadding.current)
                    .padding(margin),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(UiR.drawable.pulse_logo), null, Modifier.size(40.dp))
                    PulseText(
                        stringResource(R.string.pulse_home_brand),
                        Modifier.padding(start = PulseTokens.small),
                        PulseTextStyle.Heading2,
                        colors.textPrimary,
                    )
                    Spacer(Modifier.weight(1f))
                    PulseIconButton(stringResource(R.string.pulse_home_notifications), onNotifications) {
                        Icon(painterResource(UiR.drawable.pulse_bell), null)
                    }
                }
                DashboardClock()
                Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                    val name = (state.profile as? Section.Content)?.value?.name ?: username
                    PulseText(
                        stringResource(R.string.pulse_home_good_to_see_you),
                        style = PulseTextStyle.Label,
                        color = colors.textSecondary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulseText(
                            name,
                            Modifier.weight(1f, fill = false).semantics { heading() },
                            PulseTextStyle.Heading1,
                            colors.textPrimary,
                        )
                        PulseText(
                            stringResource(R.string.pulse_home_wave),
                            Modifier.clearAndSetSemantics {},
                            PulseTextStyle.Heading1,
                        )
                    }
                    PulseText(
                        stringResource(R.string.pulse_home_a_little_perspective_on_your_everyday),
                        color = colors.textSecondary,
                    )
                }
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

internal fun formatDashboardDateTime(
    instant: Instant,
    locale: Locale = Locale.getDefault(),
): String =
    DateTimeFormatter
        .ofPattern("EEEE, d MMM yyyy · h:mm a", locale)
        .withZone(ZoneId.of("Asia/Kolkata"))
        .format(instant)

@Composable
private fun DashboardClock() {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now by produceState(Instant.now(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = Instant.now()
                delay(60_000 - value.toEpochMilli() % 60_000)
            }
        }
    }
    PulseText(formatDashboardDateTime(now), color = PulseTheme.colors.textSecondary)
}
