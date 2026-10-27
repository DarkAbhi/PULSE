package dev.abhishekan.pulse.feature.fitness
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.pulseDescription
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun FitnessRoute(
    onWorkout: (Long) -> Unit,
    viewModel: FitnessViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PulseSnackbarEffect(state.message.takeUnless { state.needsReview }, viewModel::consumeMessage)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.resume() }
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
        val wide = size == PulseWindowSize.Expanded && LocalDensity.current.fontScale < 1.5f
        val pullState = rememberPullToRefreshState()
        val canRefresh = !state.loading && !state.busy
        val refresh = { if (canRefresh) onRefresh() }
        val refreshLabel = stringResource(R.string.pulse_fitness_refresh_activity)
        PullToRefreshBox(
            isRefreshing = state.loading,
            state = pullState,
            indicator = {
                if (state.overview != null || !state.loading) {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = state.loading,
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
            if (state.loading && state.overview == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PulseLoadingState(
                        stringResource(R.string.pulse_fitness_refreshing_your_activity),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    )
                }
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
                    state.error?.let {
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
                            FitnessStats(state.overview)
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
    val locale = LocalConfiguration.current.locales[0]
    val workouts = overview.workouts.filter { it.date == state.selected }
    var confirmRecord by rememberSaveable(state.selected) { mutableStateOf(false) }
    val canRecord = !state.loading && !state.busy && state.error == null && !state.needsReview
    if (confirmRecord && canRecord) {
        PulseDialog(
            title = stringResource(R.string.pulse_fitness_confirm_record_title),
            message =
                stringResource(
                    R.string.pulse_fitness_confirm_record_message,
                    state.selected.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale)),
                ),
            confirmLabel = stringResource(R.string.pulse_fitness_record_workout),
            onConfirm = {
                confirmRecord = false
                onRecord()
            },
            onDismiss = { confirmRecord = false },
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
            PulseText(
                stringResource(R.string.pulse_fitness_selected_day),
                style = PulseTextStyle.Overline,
                color = PulseTheme.colors.action,
            )
            PulseText(
                state.selected.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale)),
                Modifier.semantics { heading() },
                style = PulseTextStyle.Title,
            )
            Row(
                Modifier.padding(vertical = PulseTokens.small),
                horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WorkoutBadge()
                PulseText(
                    pluralStringResource(R.plurals.pulse_fitness_workouts_logged, workouts.size, workouts.size),
                    style = PulseTextStyle.Label,
                )
            }
            PulseButton(
                stringResource(R.string.pulse_fitness_add_workout),
                { confirmRecord = true },
                Modifier.fillMaxWidth(),
                enabled = !state.loading && state.error == null && !state.needsReview,
                loading = state.busy,
            )
        }
        PulseCard(Modifier.fillMaxWidth()) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium),
            ) {
                PulseText(
                    stringResource(R.string.pulse_fitness_workouts_for_day),
                    Modifier.weight(1f).semantics { heading() },
                    PulseTextStyle.Label,
                )
                PulseText(
                    pluralStringResource(R.plurals.pulse_fitness_workout_count, workouts.size, workouts.size),
                    style = PulseTextStyle.Caption,
                    color = PulseTheme.colors.textSecondary,
                )
            }
            if (workouts.isEmpty()) {
                PulseText(
                    stringResource(R.string.pulse_fitness_no_workouts_for_day),
                    Modifier.padding(vertical = PulseTokens.medium),
                    color = PulseTheme.colors.textSecondary,
                )
            }
            workouts.forEachIndexed { index, workout ->
                Surface(
                    onClick = { onWorkout(workout.id) },
                    enabled = !state.busy,
                    modifier =
                        Modifier.fillMaxWidth().pulseDescription(
                            stringResource(
                                R.string.pulse_fitness_view_workout_1_s_2_s_3_s,
                                index + 1,
                                workout.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale)),
                                workout.timeLabel,
                            ),
                        ),
                    shape = PulseTokens.cardShape,
                    color = PulseTheme.colors.surface,
                    border =
                        BorderStroke(
                            1.dp,
                            PulseTheme.colors.divider,
                        ),
                ) {
                    Row(
                        Modifier.padding(PulseTokens.medium),
                        horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WorkoutBadge()
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny)) {
                            PulseText(
                                if (workouts.size == 1) {
                                    stringResource(R.string.pulse_fitness_workout_title)
                                } else {
                                    stringResource(R.string.pulse_fitness_workout_number, index + 1)
                                },
                                style = PulseTextStyle.Label,
                            )
                            PulseText(workout.timeLabel, color = PulseTheme.colors.textSecondary)
                        }
                        FitnessGlyph(FitnessSymbol.Next, PulseTheme.colors.textSecondary)
                    }
                }
            }
        }
        state.createdId?.takeIf { id -> workouts.none { it.id == id } }?.let { id ->
            PulseButton(stringResource(R.string.pulse_fitness_open_recorded_workout), {
                onWorkout(id)
            }, Modifier.fillMaxWidth(), enabled = !state.busy)
        }
    }
}

@Composable
private fun WorkoutBadge() {
    Surface(
        shape = CircleShape,
        color = PulseTheme.colors.primarySoft,
    ) {
        Icon(
            painterResource(dev.abhishekan.pulse.core.ui.R.drawable.pulse_fitness),
            contentDescription = null,
            modifier = Modifier.padding(PulseTokens.medium),
            tint = PulseTheme.colors.action,
        )
    }
}

@Composable
internal fun MutationReview(
    ready: Boolean,
    busy: Boolean,
    onRefresh: () -> Unit,
    onReviewed: () -> Unit,
    message: UiText? = null,
) {
    PulseCard(Modifier.fillMaxWidth()) {
        message?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
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
