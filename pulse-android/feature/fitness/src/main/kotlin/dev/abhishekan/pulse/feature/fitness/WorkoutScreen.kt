package dev.abhishekan.pulse.feature.fitness
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.domain.model.WorkoutExercise
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseRefreshIndicator
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.pagePadding
import org.koin.androidx.compose.koinViewModel

@Composable
fun WorkoutRoute(
    onDeleted: () -> Unit,
    onDraftChanged: (Boolean) -> Unit,
    viewModel: WorkoutViewModel = koinViewModel(),
    onEditorTitleChanged: (String?) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editorTitle =
        state.editor?.let {
            stringResource(
                if (it.draft.exerciseId ==
                    null
                ) {
                    R.string.pulse_fitness_editor_add_title
                } else {
                    R.string.pulse_fitness_edit_exercise
                },
            )
        }
    LaunchedEffect(editorTitle) { onEditorTitleChanged(editorTitle) }
    DisposableEffect(Unit) {
        onDispose { onEditorTitleChanged(null) }
    }
    PulseSnackbarEffect(state.message.takeUnless { state.needsReview }, viewModel::consumeMessage)
    var discard by rememberSaveable { mutableStateOf(false) }
    val close = {
        if (!state.busy) {
            if (state.editor?.dirty ==
                true
            ) {
                discard = true
            } else {
                viewModel.closeEditor()
            }
        }
    }
    BackHandler(enabled = state.editor != null || state.busy) { close() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.resume() }
    LaunchedEffect(state.editor?.dirty) { onDraftChanged(state.editor?.dirty == true) }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }
    WorkoutScreen(
        state,
        {
            viewModel.refresh()
        },
        viewModel::edit,
        viewModel::removeWorkout,
        viewModel::removeExercise,
        viewModel::acknowledgeReview,
        viewModel::nameChanged,
        viewModel::setChanged,
        viewModel::addSet,
        viewModel::removeSet,
        viewModel::choose,
        viewModel::clearMatch,
        viewModel::findMatches,
        viewModel::rememberAlias,
        viewModel::save,
        close,
        onSearchQuery = viewModel::searchCatalogue,
    )
    if (discard) {
        PulseDialog(
            stringResource(R.string.pulse_fitness_discard_exercise_edits),
            stringResource(R.string.pulse_fitness_your_unsaved_exercise_name_and_sets_will_be_lost),
            stringResource(R.string.pulse_fitness_discard_edits),
            {
                discard = false
                viewModel.closeEditor()
            },
            {
                discard =
                    false
            },
        )
    }
}

@Composable
fun WorkoutScreen(
    state: WorkoutUiState,
    onRefresh: () -> Unit,
    onEdit: (Long?) -> Unit,
    onDelete: () -> Unit,
    onDeleteExercise: (Long) -> Unit,
    onReviewed: () -> Unit,
    onName: (String) -> Unit,
    onSet: (Int, String?, String?) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onChoose: (CatalogExercise) -> Unit,
    onClearMatch: () -> Unit,
    onSearch: () -> Unit,
    onAlias: (Boolean) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onSearchQuery: (String) -> Unit = onName,
) {
    var deleteWorkout by rememberSaveable { mutableStateOf(false) }
    var deleteExercise by rememberSaveable { mutableStateOf<Long?>(null) }
    val detail = state.detail
    val scroll = key(state.editor != null) { rememberScrollState() }
    PulseAdaptiveLayout(Modifier.fillMaxSize()) { size ->
        if (state.loading && detail == null && state.editor == null) {
            Box(
                Modifier.fillMaxSize().padding(LocalPulseContentPadding.current),
                contentAlignment = Alignment.Center,
            ) {
                PulseLoadingState(
                    stringResource(R.string.pulse_fitness_loading_workout),
                    horizontalAlignment = Alignment.CenterHorizontally,
                )
            }
            return@PulseAdaptiveLayout
        }
        val pullState = rememberPullToRefreshState()
        val canRefresh = !state.loading && !state.busy && state.editor == null
        val refreshLabel = stringResource(R.string.pulse_fitness_refresh_workout)
        Box(
            Modifier
                .fillMaxSize()
                .padding(LocalPulseContentPadding.current)
                .testTag("workout-refresh")
                .pullToRefresh(
                    isRefreshing = state.loading && state.editor == null,
                    state = pullState,
                    enabled = canRefresh,
                    onRefresh = onRefresh,
                ).semantics {
                    customActions =
                        if (canRefresh) {
                            listOf(
                                CustomAccessibilityAction(refreshLabel) {
                                    onRefresh()
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
                        max = if (state.editor != null) PulseTokens.readingWidth + PulseTokens.large * 4 else PulseTokens.contentWidth,
                    ).fillMaxSize()
                    .verticalScroll(scroll)
                    .imePadding()
                    .padding(size.pagePadding),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
            ) {
                state.error?.let { PulseErrorState(fitnessFailureMessage(it), onRefresh) }
                if (state.needsReview) {
                    MutationReview(state.reviewReady, state.busy, onRefresh, onReviewed, message = state.message)
                    detail?.let { ServerExercises(it.exercises) }
                }
                if (state.editor != null) {
                    ExerciseEditor(
                        state.editor,
                        state.busy || state.loading,
                        state.needsReview,
                        onName,
                        onSet,
                        onAddSet,
                        onRemoveSet,
                        onChoose,
                        onClearMatch,
                        onSearch,
                        onAlias,
                        onSave,
                        onCancel,
                        onSearchQuery,
                    )
                } else if (detail != null) {
                    WorkoutDetailContent(
                        detail = detail,
                        enabled = !state.busy && !state.loading && !state.needsReview && state.error == null,
                        onAdd = { onEdit(null) },
                        onEdit = { onEdit(it) },
                        onRemove = { deleteExercise = it },
                        onDelete = { deleteWorkout = true },
                    )
                    if (state.busy) {
                        PulseLoadingState(
                            stringResource(R.string.pulse_fitness_saving_your_change),
                        )
                    }
                }
            }
            if (state.editor == null) {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = state.loading,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
        if (state.loading && state.editor != null) {
            Box(
                Modifier.widthIn(max = PulseTokens.contentWidth).fillMaxWidth().padding(LocalPulseContentPadding.current),
            ) {
                PulseRefreshIndicator()
            }
        }
    }
    if (deleteWorkout) {
        PulseDialog(
            stringResource(R.string.pulse_fitness_delete_this_workout),
            stringResource(
                R.string.pulse_fitness_this_permanently_removes_this_gym_visit_and_all_its,
            ),
            stringResource(R.string.pulse_fitness_delete_workout),
            {
                deleteWorkout = false
                onDelete()
            },
            {
                deleteWorkout =
                    false
            },
        )
    }
    deleteExercise?.let { id ->
        PulseDialog(
            stringResource(R.string.pulse_fitness_delete_this_exercise),
            stringResource(
                R.string.pulse_fitness_this_permanently_removes_this_exercise_and_all_its_sets,
            ),
            stringResource(R.string.pulse_fitness_delete_exercise),
            {
                deleteExercise = null
                onDeleteExercise(id)
            },
            {
                deleteExercise =
                    null
            },
        )
    }
}

@Composable
internal fun CatalogInformation(exercise: CatalogExercise) {
    PulseText(exercise.name, style = PulseTextStyle.Label)
    exercise.equipment?.let { PulseText(stringResource(R.string.pulse_fitness_equipment_1_s, it)) }
    if (exercise.muscles.isNotEmpty()) {
        PulseText(stringResource(R.string.pulse_fitness_muscles_1_s, exercise.muscles.joinToString()))
    }
    ExerciseInstructions(exercise)
}

@Composable
private fun ServerExercises(exercises: List<WorkoutExercise>) {
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
