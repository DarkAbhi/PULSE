package dev.abhishekan.pulse.core.domain.model

sealed interface NotificationTarget {
    data object Fitness : NotificationTarget

    data object Horizon : NotificationTarget

    data object Garage : NotificationTarget

    data object MealPlan : NotificationTarget

    data class Workout(
        val id: Long,
    ) : NotificationTarget

    data class Vehicle(
        val id: Long,
    ) : NotificationTarget

    companion object {
        fun parse(path: String?): NotificationTarget? =
            when (path) {
                "/fitness" -> Fitness
                "/financial-horizon" -> Horizon
                "/garage" -> Garage
                "/meal-plan" -> MealPlan
                else -> {
                    val match = Regex("^/(fitness|garage)/([1-9][0-9]{0,18})$").matchEntire(path ?: "")
                    val id = match?.groupValues?.get(2)?.toLongOrNull()
                    if (id == null) {
                        null
                    } else if (match.groupValues[1] == "fitness") {
                        Workout(id)
                    } else {
                        Vehicle(id)
                    }
                }
            }
    }
}
