package dev.abhishekan.pulse.feature.fitness

import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.fitness.R

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
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import java.time.format.DateTimeFormatter

@Composable
fun WorkoutRoute(onDeleted: () -> Unit, onDraftChanged: (Boolean) -> Unit, viewModel: WorkoutViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var discard by rememberSaveable { mutableStateOf(false) }
    val close = { if (!state.busy) { if (state.editor?.dirty == true) discard = true else viewModel.closeEditor() } }
    BackHandler(enabled = state.editor != null || state.busy) { close() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    LaunchedEffect(state.editor?.dirty) { onDraftChanged(state.editor?.dirty == true) }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }
    WorkoutScreen(state, { viewModel.refresh() }, viewModel::edit, viewModel::removeWorkout, viewModel::removeExercise,
        viewModel::acknowledgeReview, viewModel::nameChanged, viewModel::setChanged, viewModel::addSet, viewModel::removeSet,
        viewModel::choose, viewModel::clearMatch, viewModel::findMatches, viewModel::rememberAlias, viewModel::save, close)
    if (discard) PulseDialog(uiText(R.string.pulse_fitness_discard_exercise_edits), uiText(R.string.pulse_fitness_your_unsaved_exercise_name_and_sets_will_be_lost),
        uiText(R.string.pulse_fitness_discard_edits), { discard = false; viewModel.closeEditor() }, { discard = false })
}

@Composable
fun WorkoutScreen(state: WorkoutUiState, onRefresh: () -> Unit, onEdit: (Long?) -> Unit, onDelete: () -> Unit,
    onDeleteExercise: (Long) -> Unit, onReviewed: () -> Unit, onName: (String) -> Unit,
    onSet: (Int, String?, String?) -> Unit, onAddSet: () -> Unit, onRemoveSet: (Int) -> Unit,
    onChoose: (CatalogExercise) -> Unit, onClearMatch: () -> Unit, onSearch: () -> Unit,
    onAlias: (Boolean) -> Unit, onSave: () -> Unit, onCancel: () -> Unit) {
    var deleteWorkout by rememberSaveable { mutableStateOf(false) }
    var deleteExercise by rememberSaveable { mutableStateOf<Long?>(null) }
    val detail = state.detail
    val scroll = key(state.editor != null) { rememberScrollState() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth().verticalScroll(scroll).imePadding()
            .padding(PulseTokens.medium), verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
            if (state.loading) PulseLoadingState(uiText(R.string.pulse_fitness_loading_workout))
            state.error?.let { PulseErrorState(fitnessFailureMessage(it), onRefresh) }
            state.message?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            if (state.needsReview) {
                MutationReview(state.reviewReady, state.busy, onRefresh, onReviewed)
                detail?.let { ServerExercises(it.exercises) }
            }
            if (state.editor != null) {
                ExerciseEditor(state.editor, state.busy || state.loading, state.needsReview, onName, onSet, onAddSet,
                    onRemoveSet, onChoose, onClearMatch, onSearch, onAlias, onSave, onCancel)
            } else if (detail != null) {
                WorkoutInformation(detail.info)
                PulseButton(uiText(R.string.pulse_fitness_add_exercise), { onEdit(null) }, Modifier.fillMaxWidth(), enabled = !state.busy && !state.loading && !state.needsReview && state.error == null)
                if (detail.exercises.isEmpty()) PulseEmptyState(uiText(R.string.pulse_fitness_start_with_your_first_exercise), uiText(R.string.pulse_fitness_choose_a_catalogue_exercise_or_record_your_own_name))
                detail.exercises.forEach { exercise -> PulseCard(Modifier.fillMaxWidth()) {
                    PulseText(exercise.name, Modifier.semantics { heading() }, PulseTextStyle.Title)
                    exercise.catalog?.let { CatalogInformation(it) }
                    exercise.sets.forEach { set -> PulseText(uiPlural(R.plurals.pulse_fitness_exercise_set, set.reps, set.number, set.reps, set.weight?.let { uiText(R.string.pulse_fitness_weight_kg , it.toPlainString()) } ?: uiText(R.string.pulse_fitness_bodyweight))) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                        PulseTextButton(uiText(R.string.pulse_fitness_edit_1_s , exercise.name), { onEdit(exercise.id) }, enabled = !state.busy && !state.loading && !state.needsReview)
                        PulseTextButton(uiText(R.string.pulse_fitness_delete_1_s , exercise.name), { deleteExercise = exercise.id }, enabled = !state.busy && !state.loading && !state.needsReview)
                    }
                } }
                PulseTextButton(uiText(R.string.pulse_fitness_refresh_workout), onRefresh, enabled = !state.busy && !state.loading)
                PulseTextButton(uiText(R.string.pulse_fitness_delete_workout), { deleteWorkout = true }, enabled = !state.busy && !state.loading && !state.needsReview)
                if (state.busy) PulseLoadingState(uiText(R.string.pulse_fitness_saving_your_change))
            }
        }
    }
    if (deleteWorkout) PulseDialog(uiText(R.string.pulse_fitness_delete_this_workout),
        uiText(R.string.pulse_fitness_this_permanently_removes_this_gym_visit_and_all_its),
        uiText(R.string.pulse_fitness_delete_workout), { deleteWorkout = false; onDelete() }, { deleteWorkout = false })
    deleteExercise?.let { id -> PulseDialog(uiText(R.string.pulse_fitness_delete_this_exercise), uiText(R.string.pulse_fitness_this_permanently_removes_this_exercise_and_all_its_sets),
        uiText(R.string.pulse_fitness_delete_exercise), { deleteExercise = null; onDeleteExercise(id) }, { deleteExercise = null }) }
}

@Composable
private fun WorkoutInformation(info: WorkoutInfo) {
    PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
        PulseText(uiText(R.string.pulse_fitness_your_workout), style = PulseTextStyle.Label)
        PulseText(info.createdAt.atZone(FitnessDates.zone).format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")), style = PulseTextStyle.Title)
        PulseText(if (info.imported) uiText(R.string.pulse_fitness_linked_apple_watch_workout) else uiText(R.string.pulse_fitness_recorded_gym_visit))
        val formatter = DateTimeFormatter.ofPattern("d MMM · HH:mm").withZone(FitnessDates.zone)
        info.start?.let { PulseText(uiText(R.string.pulse_fitness_started_1_s , formatter.format(it))) }
        info.end?.let { PulseText(uiText(R.string.pulse_fitness_finished_1_s , formatter.format(it))) }
        info.durationSeconds?.let { PulseText(uiText(R.string.pulse_fitness_duration_1_s , formatWorkoutDuration(it))) }
        info.calories?.let { PulseText(uiText(R.string.pulse_fitness_energy_1_s_kcal , it.toPlainString())) }
        PulseText(if (info.start == null && info.durationSeconds == null && info.calories == null) uiText(R.string.pulse_fitness_timing_and_energy_data_aren_t_available_for_this) else uiText(R.string.pulse_fitness_times_shown_in_india_time_ist))
        if (info.imported) PulseText(uiText(R.string.pulse_fitness_imported_timing_and_energy_are_read_only_you_can))
    }
}

@Composable
internal fun CatalogInformation(exercise: CatalogExercise) {
    PulseText(exercise.name, style = PulseTextStyle.Label)
    exercise.equipment?.let { PulseText(uiText(R.string.pulse_fitness_equipment_1_s , it)) }
    if (exercise.muscles.isNotEmpty()) PulseText(uiText(R.string.pulse_fitness_muscles_1_s , exercise.muscles.joinToString()))
    var instructions by rememberSaveable(exercise.id) { mutableStateOf(false) }
    if (exercise.instructions.isNotEmpty()) {
        PulseTextButton(if (instructions) uiText(R.string.pulse_fitness_hide_instructions) else uiText(R.string.pulse_fitness_exercise_instructions), { instructions = !instructions })
        if (instructions) exercise.instructions.forEachIndexed { index, text -> PulseText(uiText(R.string.pulse_fitness_numbered_instruction , index + 1, text)) }
    }
}

@Composable
private fun ServerExercises(exercises: List<WorkoutExercise>) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(uiText(R.string.pulse_fitness_latest_recorded_exercises), style = PulseTextStyle.Title)
        if (exercises.isEmpty()) PulseText(uiText(R.string.pulse_fitness_no_exercises_recorded))
        exercises.forEach { exercise ->
            PulseText(exercise.name, style = PulseTextStyle.Label)
            exercise.sets.forEach { PulseText(uiPlural(R.plurals.pulse_fitness_previous_exercise_set, it.reps, it.number, it.reps, it.weight?.let { value -> uiText(R.string.pulse_fitness_weight_kg , value.toPlainString()) } ?: uiText(R.string.pulse_fitness_bodyweight))) }
        }
    }
}
