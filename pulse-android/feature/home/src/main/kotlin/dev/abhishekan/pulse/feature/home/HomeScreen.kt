package dev.abhishekan.pulse.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeRoute(username: String, onNotifications: () -> Unit, onFitness: () -> Unit, onHorizon: () -> Unit, onGarage: () -> Unit, onMealPlan: () -> Unit, viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The backend computes today in Asia/Kolkata; refresh after returning to the app.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.retryWorkout() }
    HomeScreen(username, state, viewModel::retryProfile, viewModel::retryWorkout, viewModel::retryHorizon, viewModel::retryNotifications, onNotifications, onFitness, onHorizon, onGarage, onMealPlan)
}

@Composable
fun HomeScreen(username: String, state: HomeUiState, retryProfile: () -> Unit, retryWorkout: () -> Unit,
    retryHorizon: () -> Unit, retryNotifications: () -> Unit, onNotifications: () -> Unit, onFitness: () -> Unit, onHorizon: () -> Unit, onGarage: () -> Unit, onMealPlan: () -> Unit) {
    PulseAdaptiveLayout(Modifier.fillMaxSize()) { size ->
        val margin = if (size == PulseWindowSize.Compact) PulseTokens.medium else PulseTokens.large
        Column(Modifier.widthIn(max = PulseTokens.contentWidth).fillMaxWidth().verticalScroll(rememberScrollState()).padding(margin),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
            val name = (state.profile as? Section.Content)?.value?.name ?: username
            PulseText("Good to see you,", style = PulseTextStyle.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PulseText(name, Modifier.semantics { heading() }, PulseTextStyle.Display)
            PulseText("A little perspective on your everyday.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (val profile = state.profile) {
                is Section.Error -> {
                    PulseText(profile.failure.message)
                    PulseTextButton("Retry profile", retryProfile)
                }
                Section.Loading -> PulseLoadingState("Loading your profile")
                else -> Unit
            }
            if (size == PulseWindowSize.Compact) {
                HorizonCard(state.horizon, retryHorizon, onHorizon)
                WorkoutCard(state.workout, retryWorkout, onFitness)
                NotificationsCard(state.notifications, retryNotifications, onNotifications)
                SpacesCard(onFitness, onHorizon, onGarage, onMealPlan)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                    Column(Modifier.weight(if (size == PulseWindowSize.Expanded) 1.3f else 1f), verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                        HorizonCard(state.horizon, retryHorizon, onHorizon)
                        WorkoutCard(state.workout, retryWorkout, onFitness)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                        NotificationsCard(state.notifications, retryNotifications, onNotifications)
                        SpacesCard(onFitness, onHorizon, onGarage, onMealPlan)
                    }
                }
            }
        }
    }
}
