package dev.abhishekan.pulse.feature.mealplan

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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.MealChange
import dev.abhishekan.pulse.core.domain.model.MealPreferences
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseHideNavigation
import dev.abhishekan.pulse.core.ui.PulseScreenTransition
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseFloatingActionButton
import dev.abhishekan.pulse.core.ui.components.PulseScreenScaffold
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.rememberPullRefreshTracker
import dev.abhishekan.pulse.core.ui.uiText
import org.koin.androidx.compose.koinViewModel

@Composable
fun MealPlanRoute(
    viewModel: MealPlanViewModel = koinViewModel(),
    onboarding: MealOnboardingViewModel = koinViewModel(),
) {
    val setup by onboarding.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { onboarding.refresh() }
    val timeZone by UserTimeZone.changes.collectAsStateWithLifecycle()
    LaunchedEffect(timeZone) {
        viewModel.resume()
        viewModel.refresh()
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PulseHideNavigation(setup.record == null || setup.showJourney || state.draft != null)
    val timeForm = state.draft?.timeForm == true
    val onboardingStateHolder = rememberSaveableStateHolder()
    val plannerScrollStates = rememberSaveableStateHolder()
    LaunchedEffect(setup.showJourney, timeForm) {
        if (!setup.showJourney && !timeForm) onboardingStateHolder.removeState("meal-preferences")
    }
    PulseSnackbarEffect(state.message.takeUnless { state.reviewRequired }, viewModel::consumeMessage)
    if (timeForm || (setup.record != null && !setup.showJourney)) {
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.resume() }
    }
    // Loading and first-run setup share the root depth with the planner.
    PulseScreenTransition(
        setup to state,
        screenDepth = { (preferences, planner) ->
            when {
                planner.draft?.timeForm == true -> 2
                preferences.editing || planner.draft != null -> 1
                else -> 0
            }
        },
    ) { (preferences, planner), active ->
        val timeEditor = planner.draft?.timeForm == true
        if (!timeEditor && (preferences.record == null || preferences.showJourney)) {
            PulseScreenScaffold(title = stringResource(R.string.meal_setup_title), showTopBar = false) {
                onboardingStateHolder.SaveableStateProvider("meal-preferences") {
                    MealOnboardingScreen(preferences, onboarding, active = active) {
                        CustomMealTimes(planner, viewModel, enabled = !preferences.saving)
                    }
                }
            }
        } else {
            PulseScreenScaffold(
                title =
                    stringResource(
                        when {
                            timeEditor -> R.string.pulse_mealplan_new_meal_time
                            planner.draft == null -> R.string.meal_setup_title
                            planner.draft.id == null -> R.string.pulse_mealplan_plan_a_meal
                            else -> R.string.pulse_mealplan_edit_meal
                        },
                    ),
                showTopBar = planner.draft != null,
                backLabel =
                    stringResource(
                        if (timeEditor) R.string.meal_setup_back_to_preferences else R.string.pulse_mealplan_back_to_planner,
                    ),
                backEnabled = !planner.busy,
            ) {
                key(timeZone) {
                    MealPlanScreen(
                        planner,
                        viewModel,
                        plannerScrollStates,
                        active,
                        onboarding::editPreferences,
                        preferences.resolvedElsewhere,
                        preferences.record?.preferences,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MealPlanScreen(
    state: MealPlanUiState,
    vm: MealPlanViewModel,
    holder: SaveableStateHolder,
    active: Boolean,
    onPreferences: (() -> Unit)? = null,
    resolvedElsewhere: Boolean = false,
    preferences: MealPreferences? = null,
) {
    val draft = state.draft
    val selectableTimes =
        state.times.value.orEmpty().filter { time ->
            time.id == draft?.originalTimeId || preferences?.allows(time) != false
        }
    val save = { vm.save(selectableTimes.map { it.id }.toSet()) }
    var discard by rememberSaveable { mutableStateOf(false) }
    var records by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.draft == null) { if (state.draft == null) records = false }
    var deletion by remember { mutableStateOf<Pair<MealChange, UiText>?>(null) }
    val cancel = { if (state.draft?.dirty == true) discard = true else vm.discard() }
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
    val refresh = { if (canRefresh) tracker.request(vm::refresh) }
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
                            PulseCard(Modifier.fillMaxWidth()) {
                                state.message?.let { PulseText(it) }
                                PulseText(
                                    stringResource(R.string.pulse_mealplan_review_before_another_change),
                                    style = PulseTextStyle.Title,
                                )
                                PulseText(
                                    stringResource(
                                        R.string.pulse_mealplan_a_previous_update_may_have_reached_the_server_check,
                                    ),
                                )
                                PulseButton(
                                    stringResource(R.string.pulse_mealplan_refresh_server_records),
                                    vm::refresh,
                                    enabled = !state.busy,
                                    style = PulseButtonStyle.Text,
                                )
                                if (state.draft !=
                                    null
                                ) {
                                    PulseButton(
                                        if (records) {
                                            stringResource(
                                                R.string.pulse_mealplan_return_to_draft,
                                            )
                                        } else {
                                            stringResource(
                                                R.string.pulse_mealplan_view_server_records,
                                            )
                                        },
                                        {
                                            records =
                                                !records
                                        },
                                        style = PulseButtonStyle.Text,
                                    )
                                }
                                PulseButton(
                                    stringResource(R.string.pulse_mealplan_i_reviewed_the_server_records),
                                    vm::reviewed,
                                    enabled =
                                        state.reviewReady && !state.busy,
                                )
                            }
                        }
                        if (state.draft != null && !records) {
                            if (state.draft.timeForm) {
                                MealTimeEditor(state.draft, state.busy, state.error, vm::update, save)
                            } else {
                                state.times.error?.takeUnless { state.times.loading }?.let {
                                    PulseErrorState(it.message, vm::refreshTimes)
                                }
                                MealEditor(
                                    state.draft,
                                    selectableTimes,
                                    state.busy,
                                    state.error,
                                    vm::update,
                                    save,
                                    flat = size != PulseWindowSize.Compact,
                                )
                            }
                        } else if (state.draft?.timeForm == true) {
                            CustomMealTimes(state, vm, enabled = false)
                        } else {
                            if (state.draft !=
                                null
                            ) {
                                PulseButton(
                                    stringResource(
                                        R.string.pulse_mealplan_resume_unsaved_edits,
                                    ),
                                    {
                                        records =
                                            false
                                    },
                                    style = PulseButtonStyle.Text,
                                )
                            }
                            MealCalendar(
                                state.week,
                                state.selected,
                                state.today,
                                !state.busy && !state.reviewRequired && state.draft == null,
                                { vm.shift(-1) },
                                { vm.shift(1) },
                                vm::select,
                                vm::today,
                                onPreferences = onPreferences,
                            )
                            if (resolvedElsewhere) PulseText(stringResource(R.string.meal_setup_elsewhere))
                            MealSectionContent(state.meals, vm::refreshMeals) { meals ->
                                MealAgenda(
                                    state.selected,
                                    state.today,
                                    meals.filter { it.date == state.selected },
                                    state.canWrite &&
                                        !state.meals.loading &&
                                        state.meals.error == null &&
                                        state.draft == null,
                                    { vm.edit(MealDraft.edit(it)) },
                                    vm::toggle,
                                    {
                                        deletion =
                                            MealChange.Delete(it.id) to
                                            uiText(
                                                R.string.pulse_mealplan_remove_1_s_from_2_s_this_deletes_the,
                                                it.name,
                                                it.date,
                                            )
                                    },
                                    Modifier.fillMaxWidth(),
                                )
                            }
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
                    onClick = { vm.add(state.selected) },
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
                vm.discard()
            },
            { discard = false },
        )
    }
    deletion?.takeIf { active }?.let { (change, text) ->
        PulseDialog(
            stringResource(
                R.string.pulse_mealplan_delete_from_meal_plan,
            ),
            text.asString(),
            stringResource(R.string.pulse_mealplan_delete),
            {
                deletion =
                    null
                vm.mutate(change)
            },
            { deletion = null },
        )
    }
}

@Composable
private fun <T> MealSectionContent(
    section: MealSection<T>,
    retry: () -> Unit,
    content: @Composable (T) -> Unit,
) {
    section.error?.takeUnless { section.loading }?.let { PulseErrorState(it.message, retry) }
    section.value?.let { content(it) }
}
