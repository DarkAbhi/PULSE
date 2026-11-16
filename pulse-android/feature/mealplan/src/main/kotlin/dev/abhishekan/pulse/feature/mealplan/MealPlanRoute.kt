package dev.abhishekan.pulse.feature.mealplan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.PulseHideNavigation
import dev.abhishekan.pulse.core.ui.PulseScreenTransition
import dev.abhishekan.pulse.core.ui.components.PulseScreenScaffold
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import dev.abhishekan.pulse.feature.mealplan.mealtimes.CustomMealTimes
import dev.abhishekan.pulse.feature.mealplan.onboarding.MealOnboardingAction
import dev.abhishekan.pulse.feature.mealplan.onboarding.MealOnboardingScreen
import dev.abhishekan.pulse.feature.mealplan.onboarding.MealOnboardingViewModel
import dev.abhishekan.pulse.feature.mealplan.planner.MealChangeReview
import dev.abhishekan.pulse.feature.mealplan.planner.MealPlanAction
import dev.abhishekan.pulse.feature.mealplan.planner.MealPlanEvent
import dev.abhishekan.pulse.feature.mealplan.planner.MealPlanScreen
import dev.abhishekan.pulse.feature.mealplan.planner.MealPlanViewModel
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel

@Composable
fun MealPlanRoute() {
    val viewModel: MealPlanViewModel = koinViewModel()
    val onboarding: MealOnboardingViewModel = koinViewModel()
    val setup by onboarding.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        onboarding.onAction(MealOnboardingAction.Resume)
        viewModel.onAction(MealPlanAction.Resume)
    }
    val timeZone by UserTimeZone.changes.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel, timeZone) {
        viewModel.onAction(MealPlanAction.Enter(timeZone))
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val messages =
        remember(viewModel, lifecycle) {
            viewModel.event.flowWithLifecycle(lifecycle).map { event ->
                when (event) {
                    is MealPlanEvent.ShowMessage -> event.message
                }
            }
        }
    PulseSnackbarEffect(messages)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PulseHideNavigation(setup.record == null || setup.showJourney || state.draft != null)
    val timeForm = state.draft?.timeForm == true
    val onboardingStateHolder = rememberSaveableStateHolder()
    val plannerScrollStates = rememberSaveableStateHolder()
    LaunchedEffect(setup.showJourney, timeForm) {
        if (!setup.showJourney && !timeForm) onboardingStateHolder.removeState("meal-preferences")
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
                    MealOnboardingScreen(preferences, onboarding::onAction, active = active) {
                        if (planner.reviewRequired && planner.draft == null) {
                            MealChangeReview(planner, viewModel::onAction, showExplanation = false)
                        }
                        CustomMealTimes(
                            planner.times.value.orEmpty(),
                            planner.times.error,
                            onRetry = { viewModel.onAction(MealPlanAction.RefreshTimes) },
                            onAdd = if (planner.draft == null) ({ viewModel.onAction(MealPlanAction.AddMealTime) }) else null,
                            onDelete = if (planner.draft == null) ({ viewModel.onAction(MealPlanAction.DeleteMealTime(it)) }) else null,
                            enabled = !preferences.saving && planner.canWrite,
                        )
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
                        viewModel::onAction,
                        plannerScrollStates,
                        active,
                        { onboarding.onAction(MealOnboardingAction.EditPreferences) },
                        preferences.resolvedElsewhere,
                        preferences.record?.preferences,
                    )
                }
            }
        }
    }
}
