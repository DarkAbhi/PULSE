package dev.abhishekan.pulse.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable class FitnessOverviewDto(
    val today: String,
    val cards: List<FitnessMetricDto>? = null,
    @SerialName("calendar_workouts") val workouts: Map<String, WorkoutGroupDto>? = null,
)

@Serializable class FitnessMetricDto(
    val id: String,
    val label: String,
    val value: String,
    val subtitle: String = "",
    val tooltip: String? = null,
)

@Serializable class WorkoutGroupDto(
    val date: String,
    val workouts: List<WorkoutSummaryDto>? = null,
)

@Serializable class WorkoutSummaryDto(
    val id: Long,
    @SerialName("time_label") val timeLabel: String,
)

@Serializable class VisitDto(
    val id: Long,
    @SerialName("created_at") val createdAt: String,
)

@Serializable class WorkoutInfoDto(
    val id: Long,
    @SerialName("created_at") val createdAt: String,
    @SerialName("start_time") val start: String? = null,
    @SerialName("end_time") val end: String? = null,
    @SerialName("duration_seconds") val duration: JsonPrimitive? = null,
    @SerialName("calories_burned") val calories: JsonPrimitive? = null,
    @SerialName("synced_from_apple_watch") val imported: Boolean = false,
)

@Serializable class ExerciseDto(
    val id: Long,
    val name: String,
    @SerialName("exercise_catalog_id") val catalogId: String? = null,
    @SerialName("catalog_exercise") val catalog: CatalogExerciseDto? = null,
    val sets: List<ExerciseSetDto>? = null,
)

@Serializable class ExerciseSetDto(
    @SerialName("set_number") val number: Int,
    val reps: Int,
    val weight: JsonPrimitive? = null,
)

@Serializable class CatalogExerciseDto(
    val id: String,
    val name: String,
    val equipment: String? = null,
    val data: CatalogDataDto? = null,
    @SerialName("match_type") val matchType: String? = null,
)

@Serializable class CatalogDataDto(
    val primaryMuscles: List<String>? = null,
    val instructions: List<String>? = null,
)

@Serializable class CatalogSearchDto(
    val candidates: List<CatalogExerciseDto>? = null,
)

@Serializable class RecordVisitRequest(
    val date: String,
)

@Serializable class RecordVisitResponse(
    val id: Long,
)

@Serializable class ExerciseRequest(
    val name: String,
    val sets: List<ExerciseSetRequest>,
    @SerialName("exercise_catalog_id") val catalogId: String?,
    @SerialName("remember_alias") val rememberAlias: Boolean,
)

@Serializable class ExerciseSetRequest(
    val reps: Int,
    val weight: JsonPrimitive?,
)
