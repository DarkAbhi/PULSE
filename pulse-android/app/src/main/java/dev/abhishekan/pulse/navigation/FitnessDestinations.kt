package dev.abhishekan.pulse.navigation

import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.abhishekan.pulse.R
import dev.abhishekan.pulse.feature.fitness.overview.FitnessRoute
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutRoute
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutViewModel
import dev.abhishekan.pulse.feature.fitness.workout.editor.ExerciseEditorRoute
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.fitnessDestinations(
    nav: NavHostController,
    backToNotifications: State<String?>,
) {
    composable(AppDestination.Fitness.route) {
        FitnessRoute(
            onWorkout = { id -> nav.navigateSingleTop(AppDestination.Workout.withId(id)) },
        )
    }
    composable(
        AppDestination.Workout.route,
        arguments = listOf(navArgument("id") { type = NavType.LongType }),
    ) { workout ->
        val id = checkNotNull(workout.arguments).getLong("id")
        WorkoutRoute(
            onExercise = {
                if (nav.currentBackStackEntry == workout) {
                    nav.navigateSingleTop(AppDestination.WorkoutExercise.withId(id))
                }
            },
            onDeleted = {
                if (nav.currentBackStackEntry == workout) nav.popBackStack()
            },
            backLabel = backToNotifications.value ?: stringResource(R.string.pulse_app_back_to_fitness),
        )
    }
    composable(
        AppDestination.WorkoutExercise.route,
        arguments = listOf(navArgument("id") { type = NavType.LongType }),
    ) { exercise ->
        val id = checkNotNull(exercise.arguments).getLong("id")
        val workout =
            remember(nav, exercise) {
                nav.getBackStackEntry(AppDestination.Workout.withId(id))
            }
        val viewModel: WorkoutViewModel = koinViewModel(viewModelStoreOwner = workout)
        ExerciseEditorRoute(
            viewModel = viewModel,
            onClose = {
                if (nav.currentBackStackEntry == exercise) nav.popBackStack()
            },
        )
    }
}
