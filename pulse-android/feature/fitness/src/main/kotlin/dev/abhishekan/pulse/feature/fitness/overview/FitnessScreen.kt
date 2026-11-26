package dev.abhishekan.pulse.feature.fitness.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.patterns.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
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
    val onToday = { onAction(FitnessAction.SelectToday) }
    PulseAdaptiveLayout(Modifier.fillMaxSize()) { size ->
        val wide = size == PulseWindowSize.Expanded && LocalDensity.current.fontScale < 1.5f
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
                        if (wide) {
                            Row(
                                Modifier.padding(horizontal = size.pagePadding),
                                horizontalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
                            ) {
                                Box(Modifier.weight(1.1f)) {
                                    FitnessCalendar(state, onDate, onMonth, onToday)
                                }
                                Box(Modifier.weight(1f)) { SelectedDay(state, onRecord, onWorkout) }
                            }
                        } else {
                            FitnessCalendar(state, onDate, onMonth, onToday, Modifier.padding(horizontal = PulseTokens.tiny))
                            Box(Modifier.padding(horizontal = size.pagePadding)) {
                                SelectedDay(state, onRecord, onWorkout)
                            }
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
