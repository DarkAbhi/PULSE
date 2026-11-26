package dev.abhishekan.pulse.feature.fitness.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.patterns.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import dev.abhishekan.pulse.core.ui.pulseWindowSize
import dev.abhishekan.pulse.core.ui.rememberPullRefreshTracker
import dev.abhishekan.pulse.feature.fitness.R
import dev.abhishekan.pulse.feature.fitness.shared.MutationReview
import dev.abhishekan.pulse.feature.fitness.shared.fitnessFailureMessage
import java.time.LocalDate

@Composable
fun FitnessScreen(
    state: FitnessUiState,
    onAction: (FitnessAction) -> Unit,
    onWorkout: (Long) -> Unit,
) {
    val onRefresh = { onAction(FitnessAction.Refresh) }
    val onRecord = { onAction(FitnessAction.RecordWorkout) }
    val onReviewed = { onAction(FitnessAction.AcknowledgeReview) }
    val onDate = { date: LocalDate -> onAction(FitnessAction.SelectDate(date)) }
    val onMonth = { offset: Long -> onAction(FitnessAction.ShiftMonth(offset)) }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val size = pulseWindowSize(maxWidth)
        val wide = maxWidth >= 1024.dp && LocalDensity.current.fontScale < 1.5f
        val pullState = rememberPullToRefreshState()
        val tracker = rememberPullRefreshTracker(state.loading)
        val pullRefreshing = tracker.requested && state.loading
        val initialLoading = state.loading && state.overview == null && !pullRefreshing
        val canRefresh = !state.loading && !tracker.requested && !state.busy
        val refresh = { if (canRefresh) tracker.request(onRefresh) }
        val refreshLabel = stringResource(R.string.pulse_fitness_refresh_activity)
        PullToRefreshBox(
            isRefreshing = pullRefreshing,
            state = pullState,
            indicator = {
                if (!initialLoading) {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = pullRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            },
            onRefresh = refresh,
            modifier =
                Modifier.fillMaxSize().padding(LocalPulseContentPadding.current).semantics {
                    customActions =
                        if (canRefresh) {
                            listOf(
                                CustomAccessibilityAction(refreshLabel) {
                                    refresh()
                                    true
                                },
                            )
                        } else {
                            emptyList()
                        }
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            if (initialLoading) {
                PulseCenteredLoadingState(stringResource(R.string.pulse_fitness_refreshing_your_activity))
            } else {
                Column(
                    Modifier
                        .widthIn(
                            max = PulseTokens.contentWidth,
                        ).fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = size.pagePadding),
                    verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
                ) {
                    state.error?.takeUnless { state.loading }?.let {
                        Box(Modifier.padding(horizontal = size.pagePadding)) {
                            PulseErrorState(fitnessFailureMessage(it), onRefresh)
                        }
                    }
                    if (state.needsReview) {
                        Box(Modifier.padding(horizontal = size.pagePadding)) {
                            MutationReview(
                                state.reviewReady,
                                state.busy,
                                onRefresh,
                                onReviewed,
                                message = state.message,
                            )
                        }
                    }
                    if (state.overview != null) {
                        val overview = state.overview
                        val workoutsByDate = remember(overview.workouts) { overview.workouts.groupBy { it.date } }
                        val details: @Composable (Modifier) -> Unit = { modifier ->
                            Column(modifier, verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                                FitnessSummary(overview, Modifier.fillMaxWidth(), wide)
                                SelectedDay(
                                    date = state.selected,
                                    visits = workoutsByDate[state.selected].orEmpty(),
                                    plan =
                                        state.schedule.firstOrNull {
                                            state.selected >= overview.today &&
                                                it.day == state.selected.dayOfWeek
                                        },
                                    onWorkout = onWorkout,
                                    createdId = state.createdId,
                                    busy = state.busy,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        val pagePadding = Modifier.padding(horizontal = size.pagePadding)
                        if (wide) {
                            Row(
                                pagePadding,
                                horizontalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
                            ) {
                                FitnessCalendar(state, onDate, onMonth, onRecord, workoutsByDate, Modifier.weight(1.2f))
                                details(Modifier.weight(1f))
                            }
                        } else {
                            FitnessCalendar(state, onDate, onMonth, onRecord, workoutsByDate, pagePadding)
                            details(pagePadding)
                        }
                        Box(Modifier.padding(horizontal = size.pagePadding)) {
                            FitnessStats(state.overview, canRefresh, refresh)
                        }
                        Box(Modifier.padding(horizontal = size.pagePadding)) {
                            WorkoutScheduleCard(state, onAction)
                        }
                    }
                }
            }
        }
    }
}
