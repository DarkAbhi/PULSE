package dev.abhishekan.pulse.feature.fitness

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun FitnessRoute(onWorkout: (Long) -> Unit, viewModel: FitnessViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    FitnessScreen(state, viewModel::select, viewModel::shiftMonth, viewModel::selectToday,
        { viewModel.refresh() }, viewModel::recordSelected, viewModel::acknowledgeReview, onWorkout)
}

@Composable
fun FitnessScreen(state: FitnessUiState, onDate: (LocalDate) -> Unit, onMonth: (Long) -> Unit, onToday: () -> Unit,
    onRefresh: () -> Unit, onRecord: () -> Unit, onReviewed: () -> Unit, onWorkout: (Long) -> Unit) {
    PulseAdaptiveLayout(Modifier.fillMaxSize()) { size ->
        val wide = size != PulseWindowSize.Compact && LocalDensity.current.fontScale < 1.5f
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = PulseTokens.contentWidth).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(if (size == PulseWindowSize.Compact) PulseTokens.medium else PulseTokens.large),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                PulseText("FITNESS", style = PulseTextStyle.Label, color = MaterialTheme.colorScheme.primary)
                PulseText("Find your rhythm.", Modifier.semantics { heading() }, PulseTextStyle.Display)
                PulseText("One day, one workout, one little step forward.")
                if (state.loading) PulseLoadingState("Refreshing your activity")
                state.error?.let { PulseErrorState(fitnessFailureMessage(it), onRefresh) }
                state.message?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                if (state.needsReview) MutationReview(state.reviewReady, state.busy, onRefresh, onReviewed)
                if (state.overview != null) {
                    if (wide) Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                        Box(Modifier.weight(1.1f)) { FitnessCalendar(state, onDate, onMonth, onToday) }
                        Box(Modifier.weight(1f)) { SelectedDay(state, onRecord, onWorkout) }
                    } else {
                        FitnessCalendar(state, onDate, onMonth, onToday)
                        SelectedDay(state, onRecord, onWorkout)
                    }
                    PulseText("A little perspective", Modifier.semantics { heading() }, PulseTextStyle.Title)
                    state.overview.metrics.chunked(if (wide) 2 else 1).forEach { metrics ->
                        Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                            metrics.forEach { metric -> PulseCard(Modifier.weight(1f)) {
                                PulseText(metric.label, style = PulseTextStyle.Label)
                                PulseText(metric.value, style = PulseTextStyle.Metric)
                                if (metric.subtitle.isNotBlank()) PulseText(metric.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                metric.note?.let { PulseText(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            } }
                        }
                    }
                    PulseTextButton("Refresh activity", onRefresh, enabled = !state.loading && !state.busy)
                }
            }
        }
    }
}

@Composable
private fun SelectedDay(state: FitnessUiState, onRecord: () -> Unit, onWorkout: (Long) -> Unit) {
    val overview = checkNotNull(state.overview)
    val workouts = overview.workouts.filter { it.date == state.selected }
    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
            PulseText(if (state.selected == overview.today) "TODAY · INDIA TIME" else "YOUR SELECTED DAY", style = PulseTextStyle.Label)
            PulseText(state.selected.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")), style = PulseTextStyle.Title)
            PulseText(if (workouts.isEmpty()) "Room for movement." else "${workouts.size} workout${if (workouts.size == 1) "" else "s"} recorded.")
            PulseButton(if (workouts.isEmpty()) "Record workout" else "Record another workout", onRecord,
                Modifier.fillMaxWidth(), enabled = !state.loading && state.error == null && !state.needsReview, loading = state.busy)
        }
        if (workouts.isEmpty()) PulseEmptyState("A fresh page", "Record a visit for this date, then add your exercises and sets.")
        workouts.forEachIndexed { index, workout -> PulseCard(Modifier.fillMaxWidth()) {
            PulseText("Workout${if (workouts.size > 1) " ${index + 1}" else ""} · ${workout.timeLabel}", style = PulseTextStyle.Title)
            PulseTextButton("View workout", { onWorkout(workout.id) }, Modifier.fillMaxWidth().semantics {
                contentDescription = "View workout ${index + 1}, ${workout.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))}, ${workout.timeLabel}"
            }, enabled = !state.busy)
        } }
        state.createdId?.takeIf { id -> workouts.none { it.id == id } }?.let { id ->
            PulseButton("Open recorded workout", { onWorkout(id) }, Modifier.fillMaxWidth(), enabled = !state.busy)
        }
        PulseText("Dates follow Asia/Kolkata. Future-dated visits appear here; statistics include activity up to today.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun MutationReview(ready: Boolean, busy: Boolean, onRefresh: () -> Unit, onReviewed: () -> Unit) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText("Check before trying again", style = PulseTextStyle.Title)
        PulseText(if (ready) "The latest server records are shown below. Review them to avoid recording the same change twice."
            else "The change may already have reached Pulse. Refresh server records before another save or delete.")
        if (ready) PulseButton("I’ve reviewed the latest records", onReviewed, enabled = !busy)
        else PulseButton("Refresh server records", onRefresh, enabled = !busy)
    }
}
