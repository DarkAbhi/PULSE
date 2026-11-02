package dev.abhishekan.pulse.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable data class VehicleDto(
    val id: Long,
    val name: String,
    @SerialName("kind") val vehicleKind: String,
    @SerialName("fuel_types") val fuelTypes: List<String>,
    @SerialName("catalog_id") val catalogId: String? = null,
    @SerialName("make_name") val makeName: String = "",
    @SerialName("model_name") val modelName: String = "",
)

@Serializable data class AirDto(
    val id: Long,
    @SerialName("filled_at") val date: String,
    @SerialName("odometer_km") val odometer: JsonPrimitive? = null,
)

@Serializable data class FuelItemDto(
    @SerialName("fuel_type") val type: String,
    @SerialName("fill_type") val fill: String,
    val quantity: JsonPrimitive,
    @SerialName("unit_price") val price: JsonPrimitive,
    @SerialName("total_cost") val total: JsonPrimitive,
)

@Serializable data class FuelDto(
    val id: Long,
    @SerialName("odometer_km") val odometer: JsonPrimitive,
    @SerialName("filled_at") val date: String,
    @SerialName("station_name") val station: String? = null,
    val notes: String? = null,
    val items: List<FuelItemDto>? = null,
)

@Serializable data class ReceiptDto(
    val id: Long,
    @SerialName("file_name") val name: String,
    @SerialName("content_type") val contentType: String,
    @SerialName("size_bytes") val size: Long,
)

@Serializable data class MaintenanceActionDto(
    val component: String,
    val action: String,
    val description: String = "",
)

@Serializable data class MaintenanceIntervalDto(
    val kilometers: Int = 0,
    val duration: Int = 0,
    val unit: String = "months",
)

@Serializable data class MaintenanceScheduleDto(
    val kind: String,
    @SerialName("active_source") val activeSource: String = "default",
    val personal: MaintenanceIntervalDto = MaintenanceIntervalDto(),
    val manual: MaintenanceIntervalDto = MaintenanceIntervalDto(),
    @SerialName("manual_reference") val manualReference: String = "",
    @SerialName("default") val defaultInterval: MaintenanceIntervalDto = MaintenanceIntervalDto(),
)

@Serializable data class UpcomingMaintenanceDto(
    val kind: String,
    val status: String,
    val source: String = "default",
    @SerialName("distance_unavailable") val distanceUnavailable: Boolean = false,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("remaining_days") val remainingDays: Int? = null,
    @SerialName("remaining_km") val remainingKm: JsonPrimitive? = null,
    val progress: Float? = null,
    @SerialName("last_at") val lastAt: String? = null,
    @SerialName("last_recorded_odometer_km") val lastRecordedOdometer: JsonPrimitive? = null,
)

@Serializable data class MaintenanceDto(
    val id: Long,
    val category: String,
    val title: String,
    val amount: JsonPrimitive,
    @SerialName("occurred_at") val date: String,
    @SerialName("odometer_km") val odometer: JsonPrimitive? = null,
    @SerialName("provider_name") val provider: String? = null,
    val notes: String? = null,
    val attachments: List<ReceiptDto>? = null,
    val actions: List<MaintenanceActionDto> = emptyList(),
)

@Serializable data class EfficiencyDto(
    @SerialName("total_cost") val cost: JsonPrimitive,
    @SerialName("total_volume") val volume: JsonPrimitive,
    @SerialName("average_km_per_litre") val average: JsonPrimitive? = null,
    @SerialName("last_km_per_litre") val last: JsonPrimitive? = null,
    @SerialName("min_km_per_litre") val min: JsonPrimitive? = null,
    @SerialName("max_km_per_litre") val max: JsonPrimitive? = null,
)

@Serializable data class GarageHistoryDto(
    @SerialName("vehicle_name") val name: String,
    @SerialName("kind") val vehicleKind: String,
    @SerialName("fuel_types") val fuelTypes: List<String>,
    @SerialName("catalog_id") val catalogId: String? = null,
    @SerialName("make_name") val makeName: String = "",
    @SerialName("model_name") val modelName: String = "",
    @SerialName("front_tire_pressure_solo") val frontSolo: JsonPrimitive? = null,
    @SerialName("rear_tire_pressure_solo") val rearSolo: JsonPrimitive? = null,
    @SerialName("front_tire_pressure_pillion") val frontPillion: JsonPrimitive? = null,
    @SerialName("rear_tire_pressure_pillion") val rearPillion: JsonPrimitive? = null,
    @SerialName("front_tire_pressure") val frontLegacy: JsonPrimitive? = null,
    @SerialName("rear_tire_pressure") val rearLegacy: JsonPrimitive? = null,
    @SerialName("air_fills") val air: List<AirDto>?,
    @SerialName("fuel_fillups") val fuel: List<FuelDto>?,
    @SerialName("maintenance_records") val maintenance: List<MaintenanceDto>?,
    @SerialName("fuel_efficiency") val efficiency: Map<String, EfficiencyDto>?,
    @SerialName("coming_up") val upcoming: List<UpcomingMaintenanceDto> = emptyList(),
    @SerialName("maintenance_schedules") val schedules: List<MaintenanceScheduleDto> = emptyList(),
)

@Serializable data class VehicleCatalogOptionDto(
    val id: String,
    val name: String,
)

@Serializable data class VehicleCatalogMatchDto(
    val id: String,
    @SerialName("make_slug") val makeSlug: String,
    @SerialName("make_name") val makeName: String,
    @SerialName("model_name") val modelName: String,
)

@Serializable data class VehicleCatalogMatchResponseDto(
    val match: VehicleCatalogMatchDto? = null,
)
