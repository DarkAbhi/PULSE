package dev.abhishekan.pulse.feature.mealplan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*
import org.koin.androidx.compose.koinViewModel
import java.time.format.DateTimeFormatter

@Composable
fun MealPlanRoute(onDraftChanged: (Boolean) -> Unit, viewModel: MealPlanViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.draft?.dirty) { onDraftChanged(state.draft?.dirty == true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.resume() }
    MealPlanScreen(state, viewModel)
}

@Composable
internal fun MealPlanScreen(state: MealPlanUiState, vm: MealPlanViewModel) {
    var discard by rememberSaveable { mutableStateOf(false) }
    var records by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.draft == null) { if (state.draft == null) records = false }
    var deletion by remember { mutableStateOf<Pair<MealChange, String>?>(null) }
    val cancel = { if (state.draft?.dirty == true) discard = true else vm.discard() }
    BackHandler(state.draft != null || state.managing || state.busy) {
        when { state.busy -> Unit; state.draft != null -> cancel(); else -> vm.manage(false) }
    }
    val holder = rememberSaveableStateHolder()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = LocalDensity.current.fontScale
        val board = maxWidth / scale >= PulseTokens.readingWidth * 2
        holder.SaveableStateProvider(if (state.draft != null && !records) "editor" else if (state.managing) "times" else state.selected.toString()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()
                .padding(if (maxWidth < PulseTokens.readingWidth) PulseTokens.medium else PulseTokens.cardPadding),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                state.message?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                if (state.reviewRequired) PulseCard(Modifier.fillMaxWidth()) {
                    PulseText("Review before another change", style = PulseTextStyle.Title)
                    PulseText("A previous update may have reached the server. Check the refreshed week and meal times before retrying.")
                    PulseTextButton("Refresh server records", vm::refresh, enabled = !state.busy)
                    if (state.draft != null) PulseTextButton(if (records) "Return to draft" else "View server records", { records = !records })
                    PulseButton("I reviewed the server records", vm::reviewed, enabled = state.reviewReady && !state.busy)
                }
                if (state.draft != null && !records) {
                    if (!state.draft.timeForm) {
                        if (state.times.loading) PulseLoadingState("Loading meal times")
                        state.times.error?.let { PulseErrorState(it.message, vm::refreshTimes) }
                    }
                    MealEditor(state.draft, state.times.value.orEmpty(), state.busy, state.reviewRequired, state.error, vm::update, vm::save, cancel)
                } else if (state.managing) {
                    PulseTextButton("Back to planner", { vm.manage(false) }, enabled = !state.busy && state.draft == null)
                    MealSectionContent(state.times, vm::refreshTimes) { times -> MealTimes(times, state.canWrite && state.draft == null,
                        { vm.edit(MealDraft(timeForm = true)) }, { time -> deletion = MealChange.DeleteTime(time) to "Delete ${time.name}? Meals remain under Other meals with their saved time ranges. Only this custom meal time is removed." }) }
                } else {
                    if (state.draft != null) PulseTextButton("Resume unsaved edits", { records = false })
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                        PulseTextButton("Previous week", { vm.shift(-1) }, enabled = !state.busy && !state.reviewRequired && state.draft == null)
                        PulseTextButton("Today", vm::today, enabled = !state.busy && !state.reviewRequired && state.draft == null)
                        PulseTextButton("Next week", { vm.shift(1) }, enabled = !state.busy && !state.reviewRequired && state.draft == null)
                        PulseTextButton("Refresh", vm::refresh, enabled = !state.busy && state.draft == null)
                        PulseTextButton("Meal times", { vm.manage(true) }, enabled = !state.busy && state.draft == null)
                    }
                    val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")
                    PulseText("${state.week.format(dateFormat)} – ${state.week.plusDays(6).format(dateFormat)}", style = PulseTextStyle.Title)
                    val weekScroll = rememberLazyListState()
                    LaunchedEffect(state.selected) { weekScroll.scrollToItem(state.selected.dayOfWeek.value - 1) }
                    LazyRow(Modifier.fillMaxWidth(), state = weekScroll, horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                        items(state.days, key = { it.toEpochDay() }) { day ->
                            PulseFilterChip("${if (day == state.today) "Today · " else ""}${day.format(DateTimeFormatter.ofPattern("EEE d"))}", day == state.selected,
                                { vm.select(day) }, Modifier.semantics { contentDescription = "${day.format(mealDate)}${if (day == state.today) ", today" else ""}" },
                                enabled = !state.busy && !state.reviewRequired && state.draft == null)
                        }
                    }
                    MealSectionContent(state.meals, vm::refreshMeals) { meals ->
                        PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
                            PulseText(if (state.selected == state.today) "A little care for today" else "Make room for good meals", style = PulseTextStyle.Display)
                            val selected = meals.filter { it.date == state.selected }
                            PulseText("${selected.count { it.consumed }} / ${selected.size}", style = PulseTextStyle.Metric)
                            PulseText("Meals consumed · selected day")
                            PulseText("${meals.size} ${if (meals.size == 1) "meal" else "meals"} planned this week")
                        }
                        val agenda: @Composable (java.time.LocalDate, Modifier) -> Unit = { day, modifier ->
                            MealAgenda(day, state.today, meals.filter { it.date == day }, state.canWrite && !state.meals.loading && state.meals.error == null && state.draft == null,
                                { vm.add(day) }, { vm.edit(MealDraft.edit(it)) }, vm::toggle,
                                { deletion = MealChange.Delete(it.id) to "Remove ${it.name} from ${it.date}? This deletes the planned meal and its consumption status." }, modifier)
                        }
                        if (board) MealWeekBoard(state.days, state.selected, state.today, meals, !state.busy && !state.reviewRequired && state.draft == null, vm::select)
                        agenda(state.selected, Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth())
                    }
                }
            }
        }
    }
    if (discard) PulseDialog("Discard meal changes?", "Your unsaved input will be lost.", "Discard", { discard = false; records = false; vm.discard() }, { discard = false })
    deletion?.let { (change, text) -> PulseDialog("Delete from Meal Plan?", text, "Delete", { deletion = null; vm.mutate(change) }, { deletion = null }) }
}

@Composable
private fun <T> MealSectionContent(section: MealSection<T>, retry: () -> Unit, content: @Composable (T) -> Unit) {
    if (section.loading) PulseLoadingState("Refreshing meal plan")
    section.error?.let { PulseErrorState(it.message, retry) }
    section.value?.let { content(it) }
}
