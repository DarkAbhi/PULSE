package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.FuelFill
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageRecord
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.TirePressure
import dev.abhishekan.pulse.core.domain.model.garageCategories
import dev.abhishekan.pulse.core.domain.model.garageFillTypes
import dev.abhishekan.pulse.core.domain.model.garageFuelTypes
import dev.abhishekan.pulse.core.domain.model.garageMaintenanceActions
import dev.abhishekan.pulse.core.domain.model.validCatalogIdentity
import dev.abhishekan.pulse.core.domain.model.validVehicleDetails
import dev.abhishekan.pulse.core.domain.repository.GarageRepository
import java.math.BigDecimal

class ApplyGarageChange(
    private val repository: GarageRepository,
) {
    suspend operator fun invoke(change: GarageChange): Outcome<Unit> =
        if (valid(change)) repository.apply(change) else Outcome.Error(Failure.InvalidInput)

    fun minimumOdometer(existing: List<FuelFill>) = existing.maxOfOrNull { it.odometer }

    fun validOdometer(
        change: GarageChange,
        existing: List<FuelFill>,
    ): Boolean =
        change !is GarageChange.Fuel ||
            change.id != null ||
            minimumOdometer(existing)?.let { change.odometer >= it } != false

    fun valid(change: GarageChange): Boolean {
        fun number(
            v: BigDecimal?,
            digits: Int,
            scale: Int,
            positive: Boolean = false,
        ) = v == null ||
            (
                (if (positive) v.signum() > 0 else v.signum() >= 0) &&
                    v.stripTrailingZeros().scale() <= scale &&
                    v < BigDecimal.TEN.pow(digits - scale)
            )

        fun pressure(v: TirePressure) = listOf(v.frontSolo, v.rearSolo, v.frontPillion, v.rearPillion).all { number(it, 5, 1) }

        fun id(v: Long?) = v == null || v > 0

        fun bytes(
            v: String,
            max: Int,
        ) = v.toByteArray(Charsets.UTF_8).size <= max
        return when (change) {
            is GarageChange.SaveVehicle ->
                change.name.isNotBlank() &&
                    change.name.trim().codePointCount(0, change.name.trim().length) <= 48 &&
                    validVehicleDetails(change.vehicleKind, change.fuelTypes) &&
                    id(change.vehicle) &&
                    change.makeName.codePointCount(0, change.makeName.length) <= 200 &&
                    change.modelName.codePointCount(0, change.modelName.length) <= 200 &&
                    validCatalogIdentity(change.vehicleKind, change.catalogId) &&
                    pressure(change.pressure)
            is GarageChange.Pressure -> change.vehicle > 0 && pressure(change.value)
            is GarageChange.Schedule -> {
                val s = change.schedule
                val intervals = listOf(s.personal, s.manual)
                val selected = if (s.activeSource == "personal")s.personal else s.manual
                change.vehicle > 0 &&
                    s.kind in listOf("engine_oil", "tire_pressure", "general_service") &&
                    s.activeSource in listOf("default", "personal", "manual") &&
                    bytes(s.manualReference, 160) &&
                    intervals.all {
                        it.kilometers in 0..1000000 &&
                            it.unit in listOf("days", "months") &&
                            it.duration in 0..(if (it.unit == "days")36500 else 1200)
                    } &&
                    (s.activeSource == "default" || selected.kilometers > 0 || selected.duration > 0)
            }
            is GarageChange.Air -> change.vehicle > 0 && number(change.odometer, 12, 1)
            is GarageChange.Fuel ->
                change.vehicle > 0 &&
                    id(change.id) &&
                    number(change.odometer, 12, 1) &&
                    change.station.orEmpty().codePointCount(
                        0,
                        change.station.orEmpty().length,
                    ) <= 160 &&
                    change.items.size in 1..2 &&
                    change.items.all {
                        it.type in garageFuelTypes &&
                            it.fill in garageFillTypes &&
                            listOf(it.quantity, it.price, it.total).count { v -> v != null } >= 2 &&
                            number(it.quantity, 10, 3, true) &&
                            number(it.price, 10, 2, true) &&
                            number(it.total, 12, 2, true)
                    }
            is GarageChange.Care ->
                change.vehicle > 0 &&
                    id(change.id) &&
                    change.category in garageCategories &&
                    change.title.isNotBlank() &&
                    bytes(change.title.trim(), 160) &&
                    number(change.amount, 12, 2) &&
                    number(change.odometer, 12, 1) &&
                    bytes(change.provider.orEmpty().trim(), 160) &&
                    change.actions.size <= 100 &&
                    change.actions
                        .map { it.component to it.action }
                        .distinct()
                        .size == change.actions.size &&
                    change.actions.all {
                        it.component.matches(Regex("[a-z][a-z0-9_]{0,63}")) &&
                            it.action in garageMaintenanceActions &&
                            bytes(it.description, 500)
                    }
            is GarageChange.Delete ->
                change.vehicle > 0 &&
                    change.id > 0 &&
                    (change.kind != GarageRecord.Receipt || (change.record ?: 0) > 0)
        }
    }
}
