package dev.abhishekan.pulse.core.data
import dev.abhishekan.pulse.core.domain.model.AirFill
import dev.abhishekan.pulse.core.domain.model.FuelEfficiency
import dev.abhishekan.pulse.core.domain.model.FuelFill
import dev.abhishekan.pulse.core.domain.model.FuelItem
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.domain.model.Maintenance
import dev.abhishekan.pulse.core.domain.model.TirePressure
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.core.domain.model.validVehicleDetails
import dev.abhishekan.pulse.core.network.dto.GarageHistoryDto
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal
import java.time.Instant

private fun JsonPrimitive.decimal(): BigDecimal =
    content.toBigDecimal().also {
        require(
            it.signum() >= 0 && it.precision() <= 100 && it.scale() in -100..100,
        )
    }

private fun JsonPrimitive?.optional() = this?.takeUnless { it == JsonNull }?.decimal()

internal fun GarageHistoryDto.domain(): VehicleHistory {
    require(name.isNotBlank() && validVehicleDetails(vehicleType, fuelTypes))
    return VehicleHistory(
        name,
        TirePressure(
            (frontSolo ?: frontLegacy).optional(),
            (
                rearSolo
                    ?: rearLegacy
            ).optional(),
            frontPillion.optional(),
            rearPillion.optional(),
        ),
        air.orEmpty().map {
            require(it.id > 0)
            AirFill(it.id, Instant.parse(it.date))
        },
        fuel.orEmpty().map { f ->
            require(f.id > 0 && !f.items.isNullOrEmpty())
            FuelFill(
                f.id,
                f.odometer.decimal(),
                Instant.parse(f.date),
                f.station,
                f.notes,
                f.items.orEmpty().map {
                    FuelItem(
                        it.type,
                        it.fill,
                        it.quantity.decimal(),
                        it.price.decimal(),
                        it.total.decimal(),
                    )
                },
            )
        },
        maintenance.orEmpty().map { m ->
            require(m.id > 0 && m.title.isNotBlank())
            Maintenance(
                m.id,
                m.category,
                m.title,
                m.amount.decimal(),
                Instant.parse(
                    m.date,
                ),
                m.odometer.optional(),
                m.provider,
                m.notes,
                m.attachments.orEmpty().map {
                    require(it.id > 0 && it.size > 0 && it.name.isNotBlank())
                    GarageReceipt(it.id, it.name, it.contentType, it.size)
                },
            )
        },
        efficiency.orEmpty().mapValues { (_, s) ->
            FuelEfficiency(
                s.cost.decimal(),
                s.volume.decimal(),
                s.average.optional(),
                s.last.optional(),
                s.min.optional(),
                s.max.optional(),
            )
        },
        vehicleType,
        fuelTypes,
    )
}
