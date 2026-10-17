package dev.abhishekan.pulse.feature.home
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeRoute(
    username: String,
    onNotifications: () -> Unit,
    onFitness: () -> Unit,
    onHorizon: () -> Unit,
    onGarage: () -> Unit,
    onMealPlan: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The backend computes today in Asia/Kolkata; refresh after returning to the app.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.retryWorkout() }
    HomeScreen(
        username,
        state,
        viewModel::retryProfile,
        viewModel::retryWorkout,
        viewModel::retryHorizon,
        viewModel::retryNotifications,
        onNotifications,
        onFitness,
        onHorizon,
        onGarage,
        onMealPlan,
    )
}

@Composable
fun HomeScreen(
    username: String,
    state: HomeUiState,
    retryProfile: () -> Unit,
    retryWorkout: () -> Unit,
    retryHorizon: () -> Unit,
    retryNotifications: () -> Unit,
    onNotifications: () -> Unit,
    onFitness: () -> Unit,
    onHorizon: () -> Unit,
    onGarage: () -> Unit,
    onMealPlan: () -> Unit,
) {
    PulseAdaptiveLayout(Modifier.fillMaxSize()) { size ->
        val margin = if (size == PulseWindowSize.Compact) PulseTokens.medium else PulseTokens.large
        Column(
            Modifier
                .widthIn(
                    max = PulseTokens.contentWidth,
                ).fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(margin),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
        ) {
            val name = (state.profile as? Section.Content)?.value?.name ?: username
            PulseText(
                stringResource(R.string.pulse_home_good_to_see_you),
                style = PulseTextStyle.Label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PulseText(name, Modifier.semantics { heading() }, PulseTextStyle.Display)
            PulseText(
                stringResource(R.string.pulse_home_a_little_perspective_on_your_everyday),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (val profile = state.profile) {
                is Section.Error -> {
                    PulseText(profile.failure.message.asString())
                    PulseTextButton(stringResource(R.string.pulse_home_retry_profile), retryProfile)
                }
                Section.Loading ->
                    PulseLoadingState(
                        stringResource(R.string.pulse_home_loading_your_profile),
                    )
                else -> Unit
            }
            if (size == PulseWindowSize.Compact) {
                HorizonCard(state.horizon, retryHorizon, onHorizon)
                WorkoutCard(state.workout, retryWorkout, onFitness)
                NotificationsCard(state.notifications, retryNotifications, onNotifications)
                SpacesCard(onFitness, onHorizon, onGarage, onMealPlan)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                    Column(
                        Modifier.weight(
                            if (size ==
                                PulseWindowSize.Expanded
                            ) {
                                1.3f
                            } else {
                                1f
                            },
                        ),
                        verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
                    ) {
                        HorizonCard(state.horizon, retryHorizon, onHorizon)
                        WorkoutCard(state.workout, retryWorkout, onFitness)
                    }
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
                    ) {
                        NotificationsCard(state.notifications, retryNotifications, onNotifications)
                        SpacesCard(onFitness, onHorizon, onGarage, onMealPlan)
                    }
                }
            }
        }
    }
}
