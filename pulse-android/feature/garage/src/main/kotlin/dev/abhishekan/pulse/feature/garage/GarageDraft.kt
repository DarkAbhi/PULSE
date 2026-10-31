package dev.abhishekan.pulse.feature.garage

import dev.abhishekan.pulse.core.domain.model.FuelFill
import dev.abhishekan.pulse.core.domain.model.FuelItem
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.Maintenance
import dev.abhishekan.pulse.core.domain.model.TirePressure
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogMatch
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogOption
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.core.domain.model.garageFuelTypes
import dev.abhishekan.pulse.core.domain.model.garageVehicleKinds
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

enum class GarageForm { Vehicle, Fuel, Care, Pressure }

data class GarageDraft(
    val kind: GarageForm,
    val vehicle: Long = 0,
    val id: Long? = null,
    val fields: Map<String, String> = emptyMap(),
    val dirty: Boolean = false,
) {
    operator fun get(key: String) = fields[key].orEmpty()

    val vehicleStep get() = get("vehicleStep").toIntOrNull()?.coerceIn(0, 2) ?: 0
    val vehicleIdentityValid get() =
        get("vehicleKind") in garageVehicleKinds &&
            get("name").isNotBlank() &&
            get("name").trim().codePointCount(0, get("name").trim().length) <= 48 &&
            (get("catalogMode") == "manual" || get("catalogId").isNotBlank()) &&
            listOf("makeName", "modelName").all { get(it).codePointCount(0, get(it).length) <= 200 }
    val vehicleFuelValid get() =
        get("vehicleFuel") in garageFuelTypes &&
            (get("dualTank") != "true" || (get("vehicleSecondFuel") in garageFuelTypes && get("vehicleSecondFuel") != get("vehicleFuel")))

    fun validPressure(key: String): Boolean {
        val value = get(key).toBigDecimalOrNull() ?: return false
        return value.signum() >= 0 && value < "10000".toBigDecimal() && value.stripTrailingZeros().scale() <= 1
    }

    fun adjustPressure(
        key: String,
        amount: Int,
    ): GarageDraft {
        if (get(key).isNotBlank() && !validPressure(key)) return this
        val value = (get(key).toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO) + amount.toBigDecimal()
        return set(key, value.coerceIn(java.math.BigDecimal.ZERO, "9999.9".toBigDecimal()).stripTrailingZeros().toPlainString())
    }

    fun useMatch(match: VehicleCatalogMatch): GarageDraft =
        copy(
            fields =
                fields - listOf("makeQuery", "modelQuery") +
                    mapOf("catalogMode" to "catalog", "makeSlug" to match.makeSlug, "makeName" to match.makeName),
        ).chooseModel(VehicleCatalogOption(match.id, match.modelName))

    fun set(
        key: String,
        value: String,
    ): GarageDraft {
        if (key in listOf("vehicleKind", "catalogMode") && get(key) == value) return this
        val cleared =
            when (key) {
                "vehicleKind", "catalogMode" -> listOf("catalogId", "makeSlug", "makeName", "modelName", "makeQuery", "modelQuery")
                else -> emptyList()
            }
        var updated = fields - cleared + (key to value)
        val previousName = listOf(get("makeName"), get("modelName")).filter(String::isNotBlank).joinToString(" ")
        if (key in listOf("makeName", "modelName") && (get("name").isBlank() || get("name") == previousName.take(48))) {
            updated =
                updated +
                (
                    "name" to
                        listOf(
                            updated["makeName"].orEmpty(),
                            updated["modelName"].orEmpty(),
                        ).filter(String::isNotBlank).joinToString(" ").take(48)
                )
        }
        return copy(fields = updated, dirty = true)
    }

    fun chooseMake(option: VehicleCatalogOption) =
        copy(
            fields = fields - listOf("catalogId", "modelName", "modelQuery") + mapOf("makeSlug" to option.id, "makeName" to option.name),
            dirty = true,
        )

    fun chooseModel(option: VehicleCatalogOption): GarageDraft {
        val defaultName = listOf(get("makeName"), option.name).filter(String::isNotBlank).joinToString(" ")
        val previousDefault = listOf(get("makeName"), get("modelName")).filter(String::isNotBlank).joinToString(" ")
        return copy(
            fields =
                fields +
                    mapOf(
                        "catalogId" to option.id,
                        "modelName" to option.name,
                        "name" to if (get("name").isBlank() || get("name") == previousDefault) defaultName.take(48) else get("name"),
                    ),
            dirty = true,
        )
    }

    fun saved() =
        arrayListOf(
            kind.name,
            vehicle.toString(),
            id?.toString().orEmpty(),
            dirty.toString(),
        ).apply {
            fields.forEach { (k, v) ->
                add(k)
                add(v)
            }
        }

    fun change(): GarageChange {
        fun number(key: String) = get(key).trim().takeIf(String::isNotEmpty)?.toBigDecimal()

        fun pressure() =
            TirePressure(
                number("frontSolo"),
                number("rearSolo"),
                number("frontPillion"),
                number("rearPillion"),
            )

        fun date(): Instant {
            val zone = ZoneId.of(get("zone").ifBlank { UserTimeZone.zone.id })
            val local = LocalDate.parse(get("date")).atTime(LocalTime.parse(get("time")))
            val original = get("originalInstant").takeIf(String::isNotBlank)?.let(Instant::parse)
            if (original?.atZone(zone)?.toLocalDateTime() == local) return original
            require(zone.rules.getValidOffsets(local).isNotEmpty())
            return local.atZone(zone).toInstant()
        }
        return when (kind) {
            GarageForm.Vehicle ->
                GarageChange.SaveVehicle(
                    get("name"),
                    get("active") != "false",
                    pressure(),
                    get("vehicleKind"),
                    if (get("dualTank") == "true") listOf(get("vehicleFuel"), get("vehicleSecondFuel")) else listOf(get("vehicleFuel")),
                    if (get("catalogMode") != "manual") get("catalogId").also { require(it.isNotBlank()) } else null,
                    get("makeName"),
                    get("modelName"),
                    id,
                )
            GarageForm.Pressure -> GarageChange.Pressure(vehicle, pressure())
            GarageForm.Fuel ->
                GarageChange.Fuel(
                    vehicle,
                    id,
                    requireNotNull(number("odometer")),
                    date(),
                    get("station"),
                    get("notes"),
                    (0 until (get("tanks").toIntOrNull() ?: 1)).map {
                        FuelItem(
                            get("type$it"),
                            get("fill$it"),
                            number("quantity$it"),
                            number("price$it"),
                            number("total$it"),
                        )
                    },
                )
            GarageForm.Care ->
                GarageChange.Care(
                    vehicle,
                    id,
                    get(
                        "category",
                    ),
                    get(
                        "title",
                    ),
                    requireNotNull(
                        number("amount"),
                    ),
                    date(),
                    number("odometer"),
                    get("provider"),
                    get("notes"),
                )
        }
    }

    companion object {
        fun restore(v: ArrayList<String>) =
            GarageDraft(
                GarageForm.valueOf(v[0]),
                v[1].toLong(),
                v[2].toLongOrNull(),
                v.drop(4).chunked(2).associate {
                    it[0] to
                        it[1]
                },
                v[3].toBoolean(),
            )

        fun new(
            kind: GarageForm,
            vehicle: Long,
            now: ZonedDateTime = ZonedDateTime.now(UserTimeZone.zone),
        ) = GarageDraft(
            kind,
            vehicle,
            fields =
                mapOf(
                    "zone" to now.zone.id,
                    "date" to now.toLocalDate().toString(),
                    "time" to
                        now
                            .toLocalTime()
                            .withSecond(0)
                            .withNano(0)
                            .toString(),
                    "category" to "service",
                    "tanks" to "1",
                    "type0" to "petrol",
                    "fill0" to "full",
                    "type1" to "petrol",
                    "fill1" to "full",
                    "active" to "true",
                    "catalogMode" to "catalog",
                ),
        )
    }
}

internal fun VehicleHistory.draft(vehicle: Long) =
    GarageDraft(
        GarageForm.Vehicle,
        vehicle,
        vehicle,
        pressure.fields() +
            mapOf(
                "name" to name,
                "vehicleKind" to vehicleKind.takeUnless { it == "unknown" }.orEmpty(),
                "vehicleFuel" to fuelTypes.first(),
                "vehicleSecondFuel" to fuelTypes.getOrNull(1).orEmpty(),
                "dualTank" to (fuelTypes.size == 2).toString(),
                "catalogMode" to if (catalogId == null) "manual" else "catalog",
                "catalogId" to catalogId.orEmpty(),
                "makeSlug" to catalogId?.split('/')?.getOrNull(1).orEmpty(),
                "makeName" to makeName,
                "modelName" to modelName,
            ),
    )

internal fun TirePressure.fields() =
    mapOf(
        "frontSolo" to frontSolo?.toPlainString().orEmpty(),
        "rearSolo" to rearSolo?.toPlainString().orEmpty(),
        "frontPillion" to frontPillion?.toPlainString().orEmpty(),
        "rearPillion" to rearPillion?.toPlainString().orEmpty(),
    )

internal fun FuelFill.draft(vehicle: Long): GarageDraft {
    val local = date.atZone(UserTimeZone.zone)
    val fields =
        mutableMapOf(
            "zone" to local.zone.id,
            "originalInstant" to date.toString(),
            "odometer" to odometer.toPlainString(),
            "date" to local.toLocalDate().toString(),
            "time" to local.toLocalTime().toString(),
            "station" to station.orEmpty(),
            "notes" to notes.orEmpty(),
            "tanks" to items.size.toString(),
        )
    items.forEachIndexed { i, t ->
        fields.putAll(
            mapOf(
                "type$i" to t.type,
                "fill$i" to t.fill,
                "quantity$i" to t.quantity?.toPlainString().orEmpty(),
                "price$i" to t.price?.toPlainString().orEmpty(),
                "total$i" to t.total?.toPlainString().orEmpty(),
            ),
        )
    }
    return GarageDraft(GarageForm.Fuel, vehicle, id, fields)
}

internal fun Maintenance.draft(vehicle: Long): GarageDraft {
    val local = date.atZone(UserTimeZone.zone)
    return GarageDraft(
        GarageForm.Care,
        vehicle,
        id,
        mapOf(
            "zone" to local.zone.id,
            "originalInstant" to date.toString(),
            "category" to category,
            "title" to title,
            "amount" to amount.toPlainString(),
            "date" to local.toLocalDate().toString(),
            "time" to local.toLocalTime().toString(),
            "odometer" to odometer?.toPlainString().orEmpty(),
            "provider" to provider.orEmpty(),
            "notes" to notes.orEmpty(),
        ),
    )
}
