package dev.abhishekan.pulse.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.abhishekan.pulse.R
import dev.abhishekan.pulse.core.ui.R as UiR

internal enum class AppDestination(
    val route: String,
    @param:StringRes val label: Int = R.string.pulse_app_pulse,
    @param:DrawableRes val icon: Int? = null,
) {
    Home("home", R.string.pulse_app_home, UiR.drawable.pulse_home),
    Fitness("fitness", R.string.pulse_app_fitness, UiR.drawable.pulse_fitness),
    Horizon("horizon", R.string.pulse_app_horizon, UiR.drawable.pulse_horizon),
    Garage("garage", R.string.pulse_app_garage, UiR.drawable.pulse_garage),
    MealPlan("mealplan", R.string.pulse_app_meals, UiR.drawable.pulse_meals),
    Profile("you?editName={editName}&fromHome={fromHome}", R.string.pulse_app_you, UiR.drawable.pulse_person),
    Workout("workout/{id}"),
    WorkoutExercise("workout/{id}/exercise"),
    Vehicle("vehicle/{id}"),
    Notifications("notifications"),
    Quests("quests"),
    Gallery("gallery"),
    ;

    val path get() = route.substringBefore('?')
    val tab get() =
        when (this) {
            Workout, WorkoutExercise -> Fitness
            Vehicle -> Garage
            Notifications, Quests -> Home
            else -> this
        }

    fun withId(id: Long) = route.replace("{id}", id.toString())

    companion object {
        val tabs = entries.filter { it.icon != null }
        val bottomTabs = tabs.filterNot { it == Profile }

        fun fromRoute(route: String?) = entries.find { it.route == route }
    }
}
