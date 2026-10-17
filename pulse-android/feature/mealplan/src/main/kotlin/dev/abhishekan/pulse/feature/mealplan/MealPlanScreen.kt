package dev.abhishekan.pulse.feature.mealplan

import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.mealplan.R

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
    var deletion by remember { mutableStateOf<Pair<MealChange, UiText>?>(null) }
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
                    PulseText(uiText(R.string.pulse_mealplan_review_before_another_change), style = PulseTextStyle.Title)
                    PulseText(uiText(R.string.pulse_mealplan_a_previous_update_may_have_reached_the_server_check))
                    PulseTextButton(uiText(R.string.pulse_mealplan_refresh_server_records), vm::refresh, enabled = !state.busy)
                    if (state.draft != null) PulseTextButton(if (records) uiText(R.string.pulse_mealplan_return_to_draft) else uiText(R.string.pulse_mealplan_view_server_records), { records = !records })
                    PulseButton(uiText(R.string.pulse_mealplan_i_reviewed_the_server_records), vm::reviewed, enabled = state.reviewReady && !state.busy)
                }
                if (state.draft != null && !records) {
                    if (!state.draft.timeForm) {
                        if (state.times.loading) PulseLoadingState(uiText(R.string.pulse_mealplan_loading_meal_times))
                        state.times.error?.let { PulseErrorState(it.message, vm::refreshTimes) }
                    }
                    MealEditor(state.draft, state.times.value.orEmpty(), state.busy, state.reviewRequired, state.error, vm::update, vm::save, cancel)
                } else if (state.managing) {
                    PulseTextButton(uiText(R.string.pulse_mealplan_back_to_planner), { vm.manage(false) }, enabled = !state.busy && state.draft == null)
                    MealSectionContent(state.times, vm::refreshTimes) { times -> MealTimes(times, state.canWrite && state.draft == null,
                        { vm.edit(MealDraft(timeForm = true)) }, { time -> deletion = MealChange.DeleteTime(time) to uiText(R.string.pulse_mealplan_delete_1_s_meals_remain_under_other_meals_with , time.name) }) }
                } else {
                    if (state.draft != null) PulseTextButton(uiText(R.string.pulse_mealplan_resume_unsaved_edits), { records = false })
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                        PulseTextButton(uiText(R.string.pulse_mealplan_previous_week), { vm.shift(-1) }, enabled = !state.busy && !state.reviewRequired && state.draft == null)
                        PulseTextButton(uiText(R.string.pulse_mealplan_today_label), vm::today, enabled = !state.busy && !state.reviewRequired && state.draft == null)
                        PulseTextButton(uiText(R.string.pulse_mealplan_next_week), { vm.shift(1) }, enabled = !state.busy && !state.reviewRequired && state.draft == null)
                        PulseTextButton(uiText(R.string.pulse_mealplan_refresh), vm::refresh, enabled = !state.busy && state.draft == null)
                        PulseTextButton(uiText(R.string.pulse_mealplan_meal_times), { vm.manage(true) }, enabled = !state.busy && state.draft == null)
                    }
                    val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")
                    PulseText(uiText(R.string.pulse_mealplan_week_range , state.week.format(dateFormat), state.week.plusDays(6).format(dateFormat)), style = PulseTextStyle.Title)
                    val weekScroll = rememberLazyListState()
                    LaunchedEffect(state.selected) { weekScroll.scrollToItem(state.selected.dayOfWeek.value - 1) }
                    LazyRow(Modifier.fillMaxWidth(), state = weekScroll, horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                        items(state.days, key = { it.toEpochDay() }) { day ->
                            PulseFilterChip(if (day == state.today) uiText(R.string.pulse_mealplan_today_date, day.format(DateTimeFormatter.ofPattern("EEE d"))) else UiText.Literal(day.format(DateTimeFormatter.ofPattern("EEE d"))), day == state.selected,
                                { vm.select(day) }, Modifier.pulseDescription(if (day == state.today) uiText(R.string.pulse_mealplan_today_date_description, day.format(mealDate)) else UiText.Literal(day.format(mealDate))),
                                enabled = !state.busy && !state.reviewRequired && state.draft == null)
                        }
                    }
                    MealSectionContent(state.meals, vm::refreshMeals) { meals ->
                        PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
                            PulseText(if (state.selected == state.today) uiText(R.string.pulse_mealplan_a_little_care_for_today) else uiText(R.string.pulse_mealplan_make_room_for_good_meals), style = PulseTextStyle.Display)
                            val selected = meals.filter { it.date == state.selected }
                            PulseText(uiText(R.string.pulse_mealplan_consumed_count , selected.count { it.consumed }, selected.size), style = PulseTextStyle.Metric)
                            PulseText(uiText(R.string.pulse_mealplan_meals_consumed_selected_day))
                            PulseText(uiPlural(R.plurals.pulse_mealplan_weekly_meals, meals.size, meals.size))
                        }
                        val agenda: @Composable (java.time.LocalDate, Modifier) -> Unit = { day, modifier ->
                            MealAgenda(day, state.today, meals.filter { it.date == day }, state.canWrite && !state.meals.loading && state.meals.error == null && state.draft == null,
                                { vm.add(day) }, { vm.edit(MealDraft.edit(it)) }, vm::toggle,
                                { deletion = MealChange.Delete(it.id) to uiText(R.string.pulse_mealplan_remove_1_s_from_2_s_this_deletes_the , it.name, it.date) }, modifier)
                        }
                        if (board) MealWeekBoard(state.days, state.selected, state.today, meals, !state.busy && !state.reviewRequired && state.draft == null, vm::select)
                        agenda(state.selected, Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth())
                    }
                }
            }
        }
    }
    if (discard) PulseDialog(uiText(R.string.pulse_mealplan_discard_meal_changes), uiText(R.string.pulse_mealplan_your_unsaved_input_will_be_lost), uiText(R.string.pulse_mealplan_discard), { discard = false; records = false; vm.discard() }, { discard = false })
    deletion?.let { (change, text) -> PulseDialog(uiText(R.string.pulse_mealplan_delete_from_meal_plan), text, uiText(R.string.pulse_mealplan_delete), { deletion = null; vm.mutate(change) }, { deletion = null }) }
}

@Composable
private fun <T> MealSectionContent(section: MealSection<T>, retry: () -> Unit, content: @Composable (T) -> Unit) {
    if (section.loading) PulseLoadingState(uiText(R.string.pulse_mealplan_refreshing_meal_plan))
    section.error?.let { PulseErrorState(it.message, retry) }
    section.value?.let { content(it) }
}
