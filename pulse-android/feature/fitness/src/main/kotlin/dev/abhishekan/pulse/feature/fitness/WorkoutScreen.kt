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
    if (discard) PulseDialog("Discard exercise edits?", "Your unsaved exercise name and sets will be lost. Saved server records will stay unchanged.",
        "Discard edits", { discard = false; viewModel.closeEditor() }, { discard = false })
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
            if (state.loading) PulseLoadingState("Loading workout")
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
                PulseButton("Add exercise", { onEdit(null) }, Modifier.fillMaxWidth(), enabled = !state.busy && !state.loading && !state.needsReview && state.error == null)
                if (detail.exercises.isEmpty()) PulseEmptyState("Start with your first exercise", "Choose a catalogue exercise or record your own name and sets.")
                detail.exercises.forEach { exercise -> PulseCard(Modifier.fillMaxWidth()) {
                    PulseText(exercise.name, Modifier.semantics { heading() }, PulseTextStyle.Title)
                    exercise.catalog?.let { CatalogInformation(it) }
                    exercise.sets.forEach { set -> PulseText("Set ${set.number} · ${set.reps} reps · ${set.weight?.let { "${it.toPlainString()} kg" } ?: "Bodyweight"}") }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                        PulseTextButton("Edit ${exercise.name}", { onEdit(exercise.id) }, enabled = !state.busy && !state.loading && !state.needsReview)
                        PulseTextButton("Delete ${exercise.name}", { deleteExercise = exercise.id }, enabled = !state.busy && !state.loading && !state.needsReview)
                    }
                } }
                PulseTextButton("Refresh workout", onRefresh, enabled = !state.busy && !state.loading)
                PulseTextButton("Delete workout", { deleteWorkout = true }, enabled = !state.busy && !state.loading && !state.needsReview)
                if (state.busy) PulseLoadingState("Saving your change")
            }
        }
    }
    if (deleteWorkout) PulseDialog("Delete this workout?",
        "This permanently removes this gym visit and all its exercises and sets. Any linked Apple Watch workout import and your exercise catalogue aliases are kept.",
        "Delete workout", { deleteWorkout = false; onDelete() }, { deleteWorkout = false })
    deleteExercise?.let { id -> PulseDialog("Delete this exercise?", "This permanently removes this exercise and all its sets from this workout. Catalogue entries and saved aliases are kept.",
        "Delete exercise", { deleteExercise = null; onDeleteExercise(id) }, { deleteExercise = null }) }
}

@Composable
private fun WorkoutInformation(info: WorkoutInfo) {
    PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
        PulseText("YOUR WORKOUT", style = PulseTextStyle.Label)
        PulseText(info.createdAt.atZone(FitnessDates.zone).format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")), style = PulseTextStyle.Title)
        PulseText(if (info.imported) "Linked Apple Watch workout" else "Recorded gym visit")
        val formatter = DateTimeFormatter.ofPattern("d MMM · HH:mm").withZone(FitnessDates.zone)
        info.start?.let { PulseText("Started · ${formatter.format(it)}") }
        info.end?.let { PulseText("Finished · ${formatter.format(it)}") }
        info.durationSeconds?.let { PulseText("Duration · ${formatWorkoutDuration(it)}") }
        info.calories?.let { PulseText("Energy · ${it.toPlainString()} kcal") }
        PulseText(if (info.start == null && info.durationSeconds == null && info.calories == null) "Timing and energy data aren’t available for this visit." else "Times shown in India time (IST).")
        if (info.imported) PulseText("Imported timing and energy are read-only. You can manage this visit’s recorded exercises below.")
    }
}

@Composable
internal fun CatalogInformation(exercise: CatalogExercise) {
    PulseText(exercise.name, style = PulseTextStyle.Label)
    exercise.equipment?.let { PulseText("Equipment · $it") }
    if (exercise.muscles.isNotEmpty()) PulseText("Muscles · ${exercise.muscles.joinToString()}")
    var instructions by rememberSaveable(exercise.id) { mutableStateOf(false) }
    if (exercise.instructions.isNotEmpty()) {
        PulseTextButton(if (instructions) "Hide instructions" else "Exercise instructions", { instructions = !instructions })
        if (instructions) exercise.instructions.forEachIndexed { index, text -> PulseText("${index + 1}. $text") }
    }
}

@Composable
private fun ServerExercises(exercises: List<WorkoutExercise>) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText("Latest recorded exercises", style = PulseTextStyle.Title)
        if (exercises.isEmpty()) PulseText("No exercises recorded.")
        exercises.forEach { exercise ->
            PulseText(exercise.name, style = PulseTextStyle.Label)
            exercise.sets.forEach { PulseText("Set ${it.number}: ${it.reps} reps, ${it.weight?.let { value -> "${value.toPlainString()} kg" } ?: "Bodyweight"}") }
        }
    }
}
