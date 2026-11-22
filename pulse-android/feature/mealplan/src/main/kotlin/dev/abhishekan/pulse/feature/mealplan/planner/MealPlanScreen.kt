package dev.abhishekan.pulse.feature.mealplan.planner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.MealPreferences
import dev.abhishekan.pulse.core.domain.model.PlannedMeal
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseFloatingActionButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.patterns.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import dev.abhishekan.pulse.core.ui.rememberPullRefreshTracker
import dev.abhishekan.pulse.feature.mealplan.R
import dev.abhishekan.pulse.feature.mealplan.editor.MealEditor
import dev.abhishekan.pulse.feature.mealplan.editor.MealEditorFooter
import dev.abhishekan.pulse.feature.mealplan.mealtimes.CustomMealTimes
import dev.abhishekan.pulse.feature.mealplan.mealtimes.MealTimeEditor

@Composable
internal fun MealPlanScreen(
    state: MealPlanUiState,
    onAction: (MealPlanAction) -> Unit,
    holder: SaveableStateHolder,
    active: Boolean,
    onPreferences: (() -> Unit)? = null,
    resolvedElsewhere: Boolean = false,
    preferences: MealPreferences? = null,
) {
    val draft = state.draft
    val selectableTimes =
        remember(state.times.value, draft?.originalTimeId, preferences) {
            state.selectableTimes(preferences)
        }
    val save = { onAction(MealPlanAction.Save(preferences)) }
    var discard by rememberSaveable { mutableStateOf(false) }
    var records by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.draft == null) { if (state.draft == null) records = false }
    var deletion by remember { mutableStateOf<PlannedMeal?>(null) }
    val cancel = { if (state.draft?.dirty == true) discard = true else onAction(MealPlanAction.Discard) }
    BackHandler(active && (state.draft != null || state.busy)) {
        if (!state.busy && state.draft != null) cancel()
    }
    val pullState = rememberPullToRefreshState()
    val refreshing = state.meals.loading || state.times.loading
    val showAddButton =
        state.draft == null &&
            state.canWrite &&
            state.meals.error == null &&
            state.meals.value != null
    val tracker = rememberPullRefreshTracker(refreshing)
    val pullRefreshing = tracker.requested && refreshing && state.draft == null
    val canRefresh = !state.busy && state.draft == null && !refreshing && !tracker.requested
    val refresh = { if (canRefresh) tracker.request { onAction(MealPlanAction.Refresh) } }
    val refreshLabel = stringResource(R.string.pulse_mealplan_refresh)
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(LocalPulseContentPadding.current)
                .pullToRefresh(
                    isRefreshing = pullRefreshing,
                    state = pullState,
                    enabled = canRefresh,
                    onRefresh = refresh,
                ).semantics {
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
    ) {
        PulseAdaptiveLayout(Modifier.align(Alignment.TopCenter).widthIn(max = PulseTokens.contentWidth).fillMaxSize()) { size ->
            holder.SaveableStateProvider(
                if (state.draft != null &&
                    !records
                ) {
                    if (draft.timeForm) "time-editor" else "editor"
                } else {
                    if (draft == null) state.selected.toString() else "review-${state.selected}"
                },
            ) {
                Column(Modifier.fillMaxSize().imePadding()) {
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(size.pagePadding)
                            .padding(bottom = if (showAddButton) 96.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
                    ) {
                        if (state.reviewRequired) {
                            MealChangeReview(state, onAction, showExplanation = true) {
                                if (draft != null) {
                                    PulseButton(
                                        stringResource(
                                            if (records) {
                                                R.string.pulse_mealplan_return_to_draft
                                            } else {
                                                R.string.pulse_mealplan_view_server_records
                                            },
                                        ),
                                        { records = !records },
                                        style = PulseButtonStyle.Text,
                                    )
                                }
                            }
                        }
                        if (state.draft != null && !records) {
                            if (state.draft.timeForm) {
                                MealTimeEditor(state.draft, state.busy, state.error, { onAction(MealPlanAction.UpdateDraft(it)) }, save)
                            } else {
                                state.times.error?.let {
                                    PulseErrorState(it.message, { onAction(MealPlanAction.RefreshTimes) })
                                }
                                MealEditor(
                                    state.draft,
                                    selectableTimes,
                                    state.busy,
                                    state.error,
                                    { onAction(MealPlanAction.UpdateDraft(it)) },
                                    save,
                                    flat = size != PulseWindowSize.Compact,
                                )
                            }
                        } else if (state.draft?.timeForm == true) {
                            CustomMealTimes(
                                state.times.value.orEmpty(),
                                state.times.error,
                                onRetry = { onAction(MealPlanAction.RefreshTimes) },
                            )
                        } else {
                            if (draft != null) {
                                PulseButton(
                                    stringResource(R.string.pulse_mealplan_resume_unsaved_edits),
                                    { records = false },
                                    style = PulseButtonStyle.Text,
                                )
                            }
                            MealPlannerContent(
                                state,
                                onAction,
                                onDelete = { deletion = it },
                                onPreferences = onPreferences,
                                resolvedElsewhere = resolvedElsewhere,
                            )
                        }
                    }
                    if (state.draft != null && !records) {
                        MealEditorFooter(
                            state.busy,
                            state.reviewRequired,
                            save,
                            cancel,
                            Modifier.padding(horizontal = size.pagePadding),
                        )
                    }
                }
            }
            if (showAddButton) {
                PulseFloatingActionButton(
                    onClick = { onAction(MealPlanAction.AddMeal) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(size.pagePadding).size(72.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.meal_add),
                        contentDescription = stringResource(R.string.pulse_mealplan_add_meal),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            if (!pullRefreshing &&
                state.meals.loading &&
                state.meals.value == null &&
                (state.draft == null || records)
            ) {
                PulseCenteredLoadingState(stringResource(R.string.pulse_mealplan_refreshing_meal_plan))
            }
            if (!pullRefreshing &&
                state.times.loading &&
                state.times.value == null &&
                state.draft != null &&
                !records &&
                !state.draft.timeForm
            ) {
                PulseCenteredLoadingState(stringResource(R.string.pulse_mealplan_loading_meal_times))
            }
        }
        if (state.draft == null) {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = pullRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
    if (active && discard) {
        PulseDialog(
            stringResource(
                R.string.pulse_mealplan_discard_meal_changes,
            ),
            stringResource(
                R.string.pulse_mealplan_your_unsaved_input_will_be_lost,
            ),
            stringResource(R.string.pulse_mealplan_discard),
            {
                discard =
                    false
                records = false
                onAction(MealPlanAction.Discard)
            },
            { discard = false },
        )
    }
    deletion?.takeIf { active }?.let { meal ->
        PulseDialog(
            stringResource(
                R.string.pulse_mealplan_delete_from_meal_plan,
            ),
            stringResource(R.string.pulse_mealplan_remove_1_s_from_2_s_this_deletes_the, meal.name, meal.date),
            stringResource(R.string.pulse_mealplan_delete),
            {
                deletion =
                    null
                onAction(MealPlanAction.DeleteMeal(meal.id))
            },
            { deletion = null },
        )
    }
}

@Composable
private fun MealPlannerContent(
    state: MealPlanUiState,
    onAction: (MealPlanAction) -> Unit,
    onDelete: (PlannedMeal) -> Unit,
    onPreferences: (() -> Unit)?,
    resolvedElsewhere: Boolean,
) {
    val canNavigate = state.canWrite && state.draft == null
    MealCalendar(
        state.week,
        state.selected,
        state.today,
        canNavigate,
        { onAction(MealPlanAction.ShiftWeek(-1)) },
        { onAction(MealPlanAction.ShiftWeek(1)) },
        { onAction(MealPlanAction.SelectDate(it)) },
        { onAction(MealPlanAction.Today) },
        onPreferences = onPreferences,
    )
    if (resolvedElsewhere) PulseText(stringResource(R.string.meal_setup_elsewhere))
    state.meals.error?.let { PulseErrorState(it.message, { onAction(MealPlanAction.RefreshMeals) }) }
    state.meals.value?.let { meals ->
        val selectedMeals = remember(meals, state.selected) { meals.filter { it.date == state.selected } }
        MealAgenda(
            state.selected,
            state.today,
            selectedMeals,
            canNavigate && state.meals is MealSection.Ready,
            { onAction(MealPlanAction.EditMeal(it)) },
            { onAction(MealPlanAction.ToggleConsumed(it)) },
            onDelete,
            Modifier.fillMaxWidth(),
        )
    }
}
