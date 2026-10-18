package dev.abhishekan.pulse.feature.mealplan
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.MealChange
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import org.koin.androidx.compose.koinViewModel

@Composable
fun MealPlanRoute(
    onDraftChanged: (Boolean) -> Unit,
    viewModel: MealPlanViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.draft?.dirty) { onDraftChanged(state.draft?.dirty == true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.resume() }
    MealPlanScreen(state, viewModel)
}

@Composable
internal fun MealPlanScreen(
    state: MealPlanUiState,
    vm: MealPlanViewModel,
) {
    val colors = PulseTheme.colors
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
    BoxWithConstraints(Modifier.fillMaxSize()) {
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
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(LocalPulseContentPadding.current)
                    .imePadding()
                    .padding(
                        if (maxWidth <
                            PulseTokens.readingWidth
                        ) {
                            PulseTokens.medium
                        } else {
                            PulseTokens.cardPadding
                        },
                    ),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
            ) {
                state.message?.let {
                    PulseText(
                        it,
                        Modifier.semantics {
                            liveRegion =
                                LiveRegionMode.Polite
                        },
                    )
                }
                if (state.reviewRequired) {
                    PulseCard(Modifier.fillMaxWidth()) {
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
                        if (state.times.loading) {
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
                    )
                } else if (state.managing) {
                    PulseTextButton(
                        stringResource(R.string.pulse_mealplan_back_to_planner),
                        {
                            vm.manage(false)
                        },
                        enabled =
                            !state.busy && state.draft == null,
                    )
                    MealSectionContent(state.times, vm::refreshTimes) { times ->
                        MealTimes(
                            times,
                            state.canWrite && state.draft == null,
                            { vm.edit(MealDraft(timeForm = true)) },
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
                    MealPlannerHeader { wide ->
                        if (wide) {
                            PulseTextButton(
                                stringResource(R.string.pulse_mealplan_previous_week),
                                {
                                    vm.shift(-1)
                                },
                                enabled =
                                    !state.busy && !state.reviewRequired && state.draft == null,
                            )
                        }
                        PulseTextButton(
                            stringResource(R.string.pulse_mealplan_today_label),
                            vm::today,
                            Modifier.drawBehind {
                                if (state.selected == state.today) {
                                    drawLine(colors.primary, Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx())
                                }
                            },
                            enabled =
                                !state.busy && !state.reviewRequired && state.draft == null,
                        )
                        if (wide) {
                            PulseTextButton(
                                stringResource(R.string.pulse_mealplan_next_week),
                                {
                                    vm.shift(1)
                                },
                                enabled =
                                    !state.busy && !state.reviewRequired && state.draft == null,
                            )
                        }
                        PulseTextButton(
                            stringResource(R.string.pulse_mealplan_refresh),
                            vm::refresh,
                            enabled =
                                !state.busy && state.draft == null,
                        )
                        PulseTextButton(
                            stringResource(R.string.pulse_mealplan_meal_times),
                            {
                                vm.manage(true)
                            },
                            enabled =
                                !state.busy && state.draft == null,
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
                    )
                    MealSectionContent(state.meals, vm::refreshMeals) { meals ->
                        MealAgenda(
                            state.selected,
                            state.today,
                            meals.filter { it.date == state.selected },
                            state.canWrite &&
                                !state.meals.loading &&
                                state.meals.error == null &&
                                state.draft == null,
                            { vm.add(state.selected) },
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
    content: @Composable (T) -> Unit,
) {
    if (section.loading) {
        PulseLoadingState(
            stringResource(R.string.pulse_mealplan_refreshing_meal_plan),
        )
    }
    section.error?.let { PulseErrorState(it.message, retry) }
    section.value?.let { content(it) }
}
