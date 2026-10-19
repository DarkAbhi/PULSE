package dev.abhishekan.pulse.feature.fitness
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.pulseDescription
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.koin.androidx.compose.koinViewModel

@Composable
fun FitnessRoute(
    onWorkout: (Long) -> Unit,
    viewModel: FitnessViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    FitnessScreen(
        state,
        viewModel::select,
        viewModel::shiftMonth,
        viewModel::selectToday,
        { viewModel.refresh() },
        viewModel::recordSelected,
        viewModel::acknowledgeReview,
        onWorkout,
    )
}

@Composable
fun FitnessScreen(
    state: FitnessUiState,
    onDate: (LocalDate) -> Unit,
    onMonth: (Long) -> Unit,
    onToday: () -> Unit,
    onRefresh: () -> Unit,
    onRecord: () -> Unit,
    onReviewed: () -> Unit,
    onWorkout: (Long) -> Unit,
) {
    PulseAdaptiveLayout(Modifier.fillMaxSize()) { size ->
        val wide = size != PulseWindowSize.Compact && LocalDensity.current.fontScale < 1.5f
        val canRefresh = !state.loading && !state.busy
        val refresh = { if (canRefresh) onRefresh() }
        val refreshLabel = stringResource(R.string.pulse_fitness_refresh_activity)
        PullToRefreshBox(
            isRefreshing = state.loading,
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
            Column(
                Modifier
                    .widthIn(
                        max = PulseTokens.contentWidth,
                    ).fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(size.pagePadding),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
            ) {
                PulseText(
                    stringResource(R.string.pulse_fitness_fitness),
                    style = PulseTextStyle.Label,
                    color = PulseTheme.colors.action,
                )
                PulseText(
                    stringResource(R.string.pulse_fitness_find_your_rhythm),
                    Modifier.semantics {
                        heading()
                    },
                    PulseTextStyle.Display,
                )
                PulseText(
                    stringResource(
                        R.string.pulse_fitness_one_day_one_workout_one_little_step_forward,
                    ),
                )
                if (state.loading) {
                    PulseLoadingState(
                        stringResource(R.string.pulse_fitness_refreshing_your_activity),
                    )
                }
                state.error?.let { PulseErrorState(fitnessFailureMessage(it), onRefresh) }
                state.message?.let {
                    PulseText(
                        it,
                        Modifier.semantics {
                            liveRegion =
                                LiveRegionMode.Polite
                        },
                    )
                }
                if (state.needsReview) {
                    MutationReview(
                        state.reviewReady,
                        state.busy,
                        onRefresh,
                        onReviewed,
                    )
                }
                if (state.overview != null) {
                    if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                            Box(Modifier.weight(1.1f)) {
                                FitnessCalendar(state, onDate, onMonth, onToday)
                            }
                            Box(Modifier.weight(1f)) { SelectedDay(state, onRecord, onWorkout) }
                        }
                    } else {
                        FitnessCalendar(state, onDate, onMonth, onToday)
                        SelectedDay(state, onRecord, onWorkout)
                    }
                    PulseText(
                        stringResource(R.string.pulse_fitness_a_little_perspective),
                        Modifier.semantics {
                            heading()
                        },
                        PulseTextStyle.Title,
                    )
                    state.overview.metrics.chunked(if (wide) 2 else 1).forEach { metrics ->
                        Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                            metrics.forEach { metric ->
                                PulseCard(Modifier.weight(1f)) {
                                    PulseText(metric.label, style = PulseTextStyle.Label)
                                    PulseText(metric.value, style = PulseTextStyle.Metric)
                                    if (metric.subtitle.isNotBlank()) {
                                        PulseText(
                                            metric.subtitle,
                                            color = PulseTheme.colors.textSecondary,
                                        )
                                    }
                                    metric.note?.let {
                                        PulseText(
                                            it,
                                            color = PulseTheme.colors.textSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDay(
    state: FitnessUiState,
    onRecord: () -> Unit,
    onWorkout: (Long) -> Unit,
) {
    val overview = checkNotNull(state.overview)
    val workouts = overview.workouts.filter { it.date == state.selected }
    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
            PulseText(
                if (state.selected ==
                    overview.today
                ) {
                    stringResource(R.string.pulse_fitness_today_india_time)
                } else {
                    stringResource(R.string.pulse_fitness_your_selected_day)
                },
                style = PulseTextStyle.Label,
            )
            PulseText(
                state.selected.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                style = PulseTextStyle.Title,
            )
            PulseText(
                if (workouts.isEmpty()) {
                    stringResource(
                        R.string.pulse_fitness_room_for_movement,
                    )
                } else {
                    pluralStringResource(
                        R.plurals.pulse_fitness_recorded_workouts,
                        workouts.size,
                        workouts.size,
                    )
                },
            )
            PulseButton(
                if (workouts.isEmpty()) {
                    stringResource(
                        R.string.pulse_fitness_record_workout,
                    )
                } else {
                    stringResource(R.string.pulse_fitness_record_another_workout)
                },
                onRecord,
                Modifier.fillMaxWidth(),
                enabled =
                    !state.loading && state.error == null && !state.needsReview,
                loading = state.busy,
            )
        }
        if (workouts.isEmpty()) {
            PulseEmptyState(
                stringResource(R.string.pulse_fitness_a_fresh_page),
                stringResource(R.string.pulse_fitness_record_a_visit_for_this_date_then_add_your),
            )
        }
        workouts.forEachIndexed { index, workout ->
            PulseCard(Modifier.fillMaxWidth()) {
                PulseText(
                    if (workouts.size >
                        1
                    ) {
                        stringResource(
                            R.string.pulse_fitness_numbered_workout_at_time,
                            index + 1,
                            workout.timeLabel,
                        )
                    } else {
                        stringResource(R.string.pulse_fitness_workout_at_time, workout.timeLabel)
                    },
                    style = PulseTextStyle.Title,
                )
                PulseTextButton(
                    stringResource(R.string.pulse_fitness_view_workout),
                    {
                        onWorkout(workout.id)
                    },
                    Modifier.fillMaxWidth().pulseDescription(
                        stringResource(
                            R.string.pulse_fitness_view_workout_1_s_2_s_3_s,
                            index + 1,
                            workout.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy")),
                            workout.timeLabel,
                        ),
                    ),
                    enabled = !state.busy,
                )
            }
        }
        state.createdId?.takeIf { id -> workouts.none { it.id == id } }?.let { id ->
            PulseButton(stringResource(R.string.pulse_fitness_open_recorded_workout), {
                onWorkout(id)
            }, Modifier.fillMaxWidth(), enabled = !state.busy)
        }
        PulseText(
            stringResource(
                R.string.pulse_fitness_dates_follow_asia_kolkata_future_dated_visits_appear_here,
            ),
            color = PulseTheme.colors.textSecondary,
        )
    }
}

@Composable
internal fun MutationReview(
    ready: Boolean,
    busy: Boolean,
    onRefresh: () -> Unit,
    onReviewed: () -> Unit,
) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(
            stringResource(R.string.pulse_fitness_check_before_trying_again),
            style = PulseTextStyle.Title,
        )
        PulseText(
            if (ready) {
                stringResource(
                    R.string.pulse_fitness_the_latest_server_records_are_shown_below_review_them,
                )
            } else {
                stringResource(
                    R.string.pulse_fitness_the_change_may_already_have_reached_pulse_refresh_server,
                )
            },
        )
        if (ready) {
            PulseButton(
                stringResource(R.string.pulse_fitness_i_ve_reviewed_the_latest_records),
                onReviewed,
                enabled = !busy,
            )
        } else {
            PulseButton(
                stringResource(R.string.pulse_fitness_refresh_server_records),
                onRefresh,
                enabled = !busy,
            )
        }
    }
}
