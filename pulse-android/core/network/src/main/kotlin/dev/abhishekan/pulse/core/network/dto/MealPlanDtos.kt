package dev.abhishekan.pulse.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MealTimeDto(
    val id: Long,
    val name: String,
    @SerialName("start_time") val start: String,
    @SerialName("end_time") val end: String,
    @SerialName("is_default") val isDefault: Boolean,
)

@Serializable
data class MealRecipeDto(
    val id: String,
    val name: String,
    @SerialName("dish_type") val dishType: String,
    @SerialName("is_ready_to_eat") val readyToEat: Boolean,
    @SerialName("ready_in_minutes") val readyInMinutes: Int? = null,
)

@Serializable
data class MealRecipePageDto(
    val recipes: List<MealRecipeDto>,
    @SerialName("has_more") val hasMore: Boolean,
)

@Serializable
data class PlannedMealDto(
    val id: Long,
    val date: String,
    val name: String,
    @SerialName("meal_time_id") val timeId: Long? = null,
    @SerialName("meal_time_name") val timeName: String? = null,
    @SerialName("start_time") val start: String? = null,
    @SerialName("end_time") val end: String? = null,
    @SerialName("is_consumed") val consumed: Boolean,
)
