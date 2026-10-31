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
