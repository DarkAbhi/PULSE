package dev.abhishekan.pulse.feature.fitness.workout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.FitnessDates
import dev.abhishekan.pulse.core.domain.model.WorkoutExercise
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseSpacing
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseExtendedFloatingActionButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.patterns.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import dev.abhishekan.pulse.core.ui.patterns.PulseLoadingState
import dev.abhishekan.pulse.core.ui.rememberPullRefreshTracker
import dev.abhishekan.pulse.feature.fitness.R
import dev.abhishekan.pulse.feature.fitness.shared.MutationReview
import dev.abhishekan.pulse.feature.fitness.shared.fitnessFailureMessage
import java.time.ZoneId

@Composable
fun WorkoutScreen(
    state: WorkoutUiState,
    onAction: (WorkoutAction) -> Unit,
    onExercise: (Long?) -> Unit,
    timeZone: ZoneId = FitnessDates.zone,
) {
    val onRefresh = { onAction(WorkoutAction.Refresh) }
    var confirmation by rememberSaveable(stateSaver = DeleteConfirmationSaver) {
        mutableStateOf<DeleteConfirmation>(DeleteConfirmation.None)
    }
    val detail = state.detail
    val exercises = detail?.exercises.orEmpty()
    val firstExerciseId = exercises.firstOrNull()?.id
    val expandedSelection =
        if (detail != null && firstExerciseId != null) {
            rememberSaveable(detail.info.id) { mutableStateOf<Long?>(firstExerciseId) }
        } else {
            null
        }
    val expandedExerciseId =
        expandedSelection?.value?.let { id -> if (exercises.any { it.id == id }) id else firstExerciseId }
    val tracker = rememberPullRefreshTracker(state.loading)
    val pullRefreshing = tracker.requested && state.loading
    val scroll = rememberLazyListState()
    var addButtonHeightPx by remember { mutableIntStateOf(0) }
    PulseAdaptiveLayout(Modifier.fillMaxSize()) { size ->
        if (state.loading && detail == null && !pullRefreshing) {
            PulseCenteredLoadingState(
                stringResource(R.string.pulse_fitness_loading_workout),
                Modifier.fillMaxSize().padding(LocalPulseContentPadding.current),
            )
            return@PulseAdaptiveLayout
        }
        val pullState = rememberPullToRefreshState()
        val canRefresh = !state.loading && !tracker.requested && !state.busy && state.editor == null
        val refreshLabel = stringResource(R.string.pulse_fitness_refresh_workout)
        val pagePadding = size.pagePadding
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(LocalPulseContentPadding.current)
                .testTag("workout-refresh")
                .pullToRefresh(
                    isRefreshing = pullRefreshing,
                    state = pullState,
                    enabled = canRefresh,
                    onRefresh = { tracker.request(onRefresh) },
                ).semantics {
                    customActions =
                        if (canRefresh) {
                            listOf(
                                CustomAccessibilityAction(refreshLabel) {
                                    tracker.request(onRefresh)
                                    true
                                },
                            )
                        } else {
                            emptyList()
                        }
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            val wide = minOf(maxWidth, PulseTokens.contentWidth) - pagePadding * 2 >= 840.dp
            val addButtonHeight = with(LocalDensity.current) { addButtonHeightPx.toDp() }
            val addButtonClearance = if (detail == null) PulseSpacing.none else addButtonHeight + PulseSpacing.small
            LazyColumn(
                modifier = Modifier.widthIn(max = PulseTokens.contentWidth).fillMaxSize().testTag("workout-list"),
                state = scroll,
                contentPadding =
                    PaddingValues(
                        start = pagePadding,
                        top = pagePadding,
                        end = pagePadding,
                        bottom = pagePadding + addButtonClearance,
                    ),
            ) {
                state.error?.let {
                    item(key = "workout-error", contentType = "error") {
                        val gap = if (detail != null || state.needsReview) PulseSpacing.cardPadding else PulseSpacing.none
                        Box(Modifier.padding(bottom = gap)) {
                            PulseErrorState(fitnessFailureMessage(it), onRefresh)
                        }
                    }
                }
                if (state.needsReview) {
                    item(key = "workout-review", contentType = "review") {
                        Box(Modifier.padding(bottom = if (detail == null) PulseSpacing.none else PulseSpacing.cardPadding)) {
                            MutationReview(
                                state.reviewReady,
                                state.busy,
                                onRefresh,
                                { onAction(WorkoutAction.AcknowledgeReview) },
                                message = state.message,
                            )
                        }
                    }
                    if (detail != null) {
                        item(key = "workout-server-exercises", contentType = "server-exercises") {
                            Box(Modifier.padding(bottom = PulseSpacing.cardPadding)) { ServerExercises(detail.exercises) }
                        }
                    }
                }
                if (detail != null) {
                    workoutDetailItems(
                        detail = detail,
                        timeZone = timeZone,
                        enabled = state.canChangeWorkout,
                        wide = wide,
                        expandedExerciseId = expandedExerciseId,
                        onToggle = { id -> expandedSelection?.value = id.takeUnless { it == expandedExerciseId } },
                        onEdit = onExercise,
                        onRemove = { confirmation = DeleteConfirmation.Exercise(it) },
                        onDelete = { confirmation = DeleteConfirmation.Workout },
                    )
                    if (state.busy && state.editor == null) {
                        item(key = "workout-saving", contentType = "saving") {
                            Box(Modifier.padding(top = PulseSpacing.cardPadding)) {
                                PulseLoadingState(stringResource(R.string.pulse_fitness_saving_your_change))
                            }
                        }
                    }
                }
            }
            if (detail != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .widthIn(max = PulseTokens.contentWidth)
                        .fillMaxWidth()
                        .padding(pagePadding),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    WorkoutAddExerciseButton(
                        enabled = state.canChangeWorkout,
                        onClick = { onExercise(null) },
                        modifier = Modifier.onSizeChanged { addButtonHeightPx = it.height },
                    )
                }
            }
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = pullRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
    WorkoutDeleteDialog(confirmation, onAction, onDismiss = { confirmation = DeleteConfirmation.None })
}

@Composable
private fun WorkoutAddExerciseButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.pulse_fitness_add_exercise)
    val icon: @Composable () -> Unit = { WorkoutGlyph(WorkoutSymbol.Add, LocalContentColor.current) }
    if (enabled) {
        PulseExtendedFloatingActionButton(
            text = { PulseText(label, style = PulseTextStyle.Label) },
            icon = icon,
            onClick = onClick,
            modifier = modifier,
        )
    } else {
        PulseButton(
            label,
            onClick,
            modifier.heightIn(min = PulseSpacing.target + PulseSpacing.small),
            enabled = false,
            leadingIcon = icon,
            contentPadding = PaddingValues(horizontal = PulseSpacing.comfortable, vertical = PulseSpacing.small),
        )
    }
}

private sealed interface DeleteConfirmation {
    data object None : DeleteConfirmation

    data object Workout : DeleteConfirmation

    data class Exercise(
        val id: Long,
    ) : DeleteConfirmation
}

private val DeleteConfirmationSaver =
    Saver<DeleteConfirmation, String>(
        save = {
            when (it) {
                DeleteConfirmation.None -> null
                DeleteConfirmation.Workout -> "workout"
                is DeleteConfirmation.Exercise -> it.id.toString()
            }
        },
        restore = { if (it == "workout") DeleteConfirmation.Workout else DeleteConfirmation.Exercise(it.toLong()) },
    )

@Composable
private fun WorkoutDeleteDialog(
    confirmation: DeleteConfirmation,
    onAction: (WorkoutAction) -> Unit,
    onDismiss: () -> Unit,
) {
    when (confirmation) {
        DeleteConfirmation.None -> Unit
        DeleteConfirmation.Workout ->
            PulseDialog(
                stringResource(R.string.pulse_fitness_delete_this_workout),
                stringResource(R.string.pulse_fitness_this_permanently_removes_this_gym_visit_and_all_its),
                stringResource(R.string.pulse_fitness_delete_workout),
                {
                    onDismiss()
                    onAction(WorkoutAction.DeleteWorkout)
                },
                onDismiss,
            )
        is DeleteConfirmation.Exercise ->
            PulseDialog(
                stringResource(R.string.pulse_fitness_delete_this_exercise),
                stringResource(R.string.pulse_fitness_this_permanently_removes_this_exercise_and_all_its_sets),
                stringResource(R.string.pulse_fitness_delete_exercise),
                {
                    onDismiss()
                    onAction(WorkoutAction.DeleteExercise(confirmation.id))
                },
                onDismiss,
            )
    }
}

@Composable
internal fun ServerExercises(exercises: List<WorkoutExercise>) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(
            stringResource(R.string.pulse_fitness_latest_recorded_exercises),
            style = PulseTextStyle.Title,
        )
        if (exercises.isEmpty()) {
            PulseText(
                stringResource(R.string.pulse_fitness_no_exercises_recorded),
            )
        }
        exercises.forEach { exercise ->
            PulseText(exercise.name, style = PulseTextStyle.Label)
            exercise.sets.forEach {
                PulseText(
                    pluralStringResource(
                        R.plurals.pulse_fitness_previous_exercise_set,
                        it.reps,
                        it.number,
                        it.reps,
                        it.weight?.let { value ->
                            stringResource(R.string.pulse_fitness_weight_kg, value.toPlainString())
                        }
                            ?: stringResource(R.string.pulse_fitness_bodyweight),
                    ),
                )
            }
        }
    }
}
