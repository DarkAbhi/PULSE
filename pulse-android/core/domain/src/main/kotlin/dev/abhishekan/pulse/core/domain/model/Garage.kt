package dev.abhishekan.pulse.core.domain.model

import java.math.BigDecimal
import java.time.Instant

const val RECEIPT_LIMIT = 10 * 1024 * 1024
val garageVehicleTypes = listOf("two_wheeler", "four_wheeler")
val garageFuelTypes = listOf("petrol", "diesel", "lpg", "cng", "electric")
val garageFillTypes = listOf("full", "partial", "missed")
val garageCategories = listOf("service", "repair", "insurance", "washing", "tyres")

data class Vehicle(
    val id: Long,
    val name: String,
    val vehicleType: String,
    val fuelTypes: List<String>,
)

data class TirePressure(
    val frontSolo: BigDecimal? = null,
    val rearSolo: BigDecimal? = null,
    val frontPillion: BigDecimal? = null,
    val rearPillion: BigDecimal? = null,
)

data class AirFill(
    val id: Long,
    val date: Instant,
)

data class FuelItem(
    val type: String,
    val fill: String,
    val quantity: BigDecimal?,
    val price: BigDecimal?,
    val total: BigDecimal?,
)

data class FuelFill(
    val id: Long,
    val odometer: BigDecimal,
    val date: Instant,
    val station: String?,
    val notes: String?,
    val items: List<FuelItem>,
)

data class GarageReceipt(
    val id: Long,
    val name: String,
    val contentType: String,
    val size: Long,
)

data class Maintenance(
    val id: Long,
    val category: String,
    val title: String,
    val amount: BigDecimal,
    val date: Instant,
    val odometer: BigDecimal?,
    val provider: String?,
    val notes: String?,
    val receipts: List<GarageReceipt>,
)

data class FuelEfficiency(
    val totalCost: BigDecimal,
    val totalVolume: BigDecimal,
    val average: BigDecimal?,
    val last: BigDecimal?,
    val min: BigDecimal?,
    val max: BigDecimal?,
)

data class VehicleHistory(
    val name: String,
    val pressure: TirePressure,
    val air: List<AirFill>,
    val fuel: List<FuelFill>,
    val maintenance: List<Maintenance>,
    val efficiency: Map<String, FuelEfficiency>,
    val vehicleType: String,
    val fuelTypes: List<String>,
)

enum class GarageRecord { Vehicle, Fuel, Air, Maintenance, Receipt }

sealed interface GarageChange {
    data class CreateVehicle(
        val name: String,
        val active: Boolean,
        val pressure: TirePressure,
        val vehicleType: String,
        val fuelTypes: List<String>,
    ) : GarageChange

    data class Pressure(
        val vehicle: Long,
        val value: TirePressure,
    ) : GarageChange

    data class Air(
        val vehicle: Long,
    ) : GarageChange

    data class Fuel(
        val vehicle: Long,
        val id: Long?,
        val odometer: BigDecimal,
        val date: Instant,
        val station: String?,
        val notes: String?,
        val items: List<FuelItem>,
    ) : GarageChange

    data class Care(
        val vehicle: Long,
        val id: Long?,
        val category: String,
        val title: String,
        val amount: BigDecimal,
        val date: Instant,
        val odometer: BigDecimal?,
        val provider: String?,
        val notes: String?,
    ) : GarageChange

    data class Delete(
        val kind: GarageRecord,
        val vehicle: Long,
        val id: Long = vehicle,
        val record: Long? = null,
    ) : GarageChange
}

fun validVehicleDetails(
    vehicleType: String,
    fuelTypes: List<String>,
): Boolean =
    vehicleType in garageVehicleTypes &&
        fuelTypes.size in 1..2 &&
        fuelTypes.all { it in garageFuelTypes } &&
        fuelTypes.distinct().size == fuelTypes.size
