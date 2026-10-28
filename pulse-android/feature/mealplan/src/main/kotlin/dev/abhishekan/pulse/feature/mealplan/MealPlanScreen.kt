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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseBackNavigation
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseRefreshIndicator
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.uiText
import org.koin.androidx.compose.koinViewModel

@Composable
fun MealPlanRoute(
    onDraftChanged: (Boolean) -> Unit,
    viewModel: MealPlanViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PulseSnackbarEffect(state.message.takeUnless { state.reviewRequired }, viewModel::consumeMessage)
    LaunchedEffect(state.draft?.dirty) { onDraftChanged(state.draft?.dirty == true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.resume() }
    MealPlanScreen(state, viewModel)
}

@Composable
internal fun MealPlanScreen(
    state: MealPlanUiState,
    vm: MealPlanViewModel,
) {
    var discard by rememberSaveable { mutableStateOf(false) }
    var records by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.draft == null) { if (state.draft == null) records = false }
    var deletion by remember { mutableStateOf<Pair<MealChange, UiText>?>(null) }
    val cancel = { if (state.draft?.dirty == true) discard = true else vm.discard() }
    BackHandler(state.draft != null || state.managing || state.busy) {
        when {
            state.busy -> Unit
            state.draft != null -> cancel()
            else -> vm.manage(false)
        }
    }
    val holder = rememberSaveableStateHolder()
    val refreshing = state.meals.loading || state.times.loading
    val showAddButton =
        !state.managing &&
            state.draft == null &&
            state.canWrite &&
            !state.meals.loading &&
            state.meals.error == null &&
            state.meals.value != null
    val canRefresh = !state.busy && state.draft == null && !refreshing
    val showAddTimeButton =
        state.managing &&
            state.draft == null &&
            state.canWrite &&
            !state.times.loading &&
            state.times.error == null &&
            state.times.value != null
    val timeActionClearance = (80 * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp
    val refresh = { if (canRefresh) vm.refresh() }
    val refreshLabel = stringResource(R.string.pulse_mealplan_refresh)
    PullToRefreshBox(
        isRefreshing = refreshing,
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
    ) {
        PulseAdaptiveLayout(Modifier.align(Alignment.TopCenter).widthIn(max = PulseTokens.contentWidth).fillMaxSize()) { size ->
            holder.SaveableStateProvider(
                if (state.draft != null &&
                    !records
                ) {
                    "editor"
                } else if (state.managing) {
                    "times"
                } else {
                    state.selected.toString()
                },
            ) {
                Column(Modifier.fillMaxSize()) {
                    if (state.draft != null && !records) {
                        PulseBackNavigation(
                            stringResource(
                                if (state.draft.timeForm) {
                                    R.string.pulse_mealplan_back_to_meal_times
                                } else {
                                    R.string.pulse_mealplan_back_to_planner
                                },
                            ),
                            cancel,
                            Modifier.fillMaxWidth().padding(horizontal = size.pagePadding),
                            enabled = !state.busy,
                        )
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                            .padding(size.pagePadding)
                            .padding(
                                bottom =
                                    if (showAddTimeButton) {
                                        timeActionClearance
                                    } else if (showAddButton) {
                                        96.dp
                                    } else {
                                        0.dp
                                    },
                            ),
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
                                PulseTextButton(
                                    stringResource(R.string.pulse_mealplan_refresh_server_records),
                                    vm::refresh,
                                    enabled = !state.busy,
                                )
                                if (state.draft !=
                                    null
                                ) {
                                    PulseTextButton(
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
                            if (!state.draft.timeForm) {
                                if (state.times.loading && state.times.value == null) {
                                    PulseLoadingState(
                                        stringResource(R.string.pulse_mealplan_loading_meal_times),
                                    )
                                }
                                state.times.error?.let { PulseErrorState(it.message, vm::refreshTimes) }
                            }
                            MealEditor(
                                state.draft,
                                state.times.value.orEmpty(),
                                state.busy,
                                state.reviewRequired,
                                state.error,
                                vm::update,
                                vm::save,
                                cancel,
                                flat = size != PulseWindowSize.Compact,
                            )
                        } else if (state.managing) {
                            MealTimesHeader({ vm.manage(false) }, enabled = !state.busy && state.draft == null)
                            MealSectionContent(state.times, vm::refreshTimes) { times ->
                                MealTimes(
                                    times,
                                    state.canWrite && state.draft == null,
                                    { time ->
                                        deletion =
                                            MealChange.DeleteTime(time) to
                                            uiText(
                                                R.string.pulse_mealplan_delete_1_s_meals_remain_under_other_meals_with,
                                                time.name,
                                            )
                                    },
                                )
                            }
                        } else {
                            if (state.draft !=
                                null
                            ) {
                                PulseTextButton(
                                    stringResource(
                                        R.string.pulse_mealplan_resume_unsaved_edits,
                                    ),
                                    {
                                        records =
                                            false
                                    },
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
                                { vm.manage(true) },
                                !state.busy && state.draft == null,
                                state.times.value?.size,
                            )
                            MealSectionContent(state.meals, vm::refreshMeals, showLoading = false) { meals ->
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
                }
            }
            if (showAddButton) {
                FloatingActionButton(
                    onClick = { vm.add(state.selected) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(size.pagePadding).size(72.dp),
                    shape = CircleShape,
                    containerColor = PulseTheme.colors.primary,
                    contentColor = PulseTheme.colors.onPrimary,
                ) {
                    Icon(
                        painterResource(R.drawable.meal_add),
                        contentDescription = stringResource(R.string.pulse_mealplan_add_meal),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            if (showAddTimeButton) {
                AddMealTimeButton(
                    { vm.edit(MealDraft(timeForm = true)) },
                    Modifier.align(Alignment.BottomEnd).padding(size.pagePadding),
                )
            }
            if (state.meals.loading && state.meals.value == null && !state.managing && (state.draft == null || records)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PulseLoadingState(
                        stringResource(R.string.pulse_mealplan_refreshing_meal_plan),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    )
                }
            }
        }
    }
    if (discard) {
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
    deletion?.let { (change, text) ->
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
    showLoading: Boolean = true,
    content: @Composable (T) -> Unit,
) {
    if (section.loading && showLoading) {
        if (section.value == null) {
            PulseLoadingState(stringResource(R.string.pulse_mealplan_refreshing_meal_plan))
        } else {
            PulseRefreshIndicator()
        }
    }
    section.error?.let { PulseErrorState(it.message, retry) }
    section.value?.let { content(it) }
}
