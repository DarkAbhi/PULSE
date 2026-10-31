package dev.abhishekan.pulse.core.data
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.domain.model.GarageRecord
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.TirePressure
import dev.abhishekan.pulse.core.domain.model.Vehicle
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogMatch
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogOption
import dev.abhishekan.pulse.core.domain.model.validCatalogIdentity
import dev.abhishekan.pulse.core.domain.model.validVehicleDetails
import dev.abhishekan.pulse.core.domain.repository.GarageRepository
import dev.abhishekan.pulse.core.domain.repository.VehicleCatalogRepository
import dev.abhishekan.pulse.core.network.GarageApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.math.BigDecimal

class PulseGarageRepository(
    private val api: GarageApi,
    private val auth: PulseAuthRepository,
    private val files: ReceiptFiles,
) : GarageRepository,
    VehicleCatalogRepository {
    private val revision = MutableStateFlow(0L)
    override val changes = revision.asStateFlow()

    private suspend fun <T> request(
        mutation: Boolean = false,
        block: suspend () -> T,
    ): Outcome<T> {
        val version = auth.version
        return try {
            val result = withContext(Dispatchers.Default) { block() }
            if (mutation) revision.update { it + 1 }
            Outcome.Success(result)
        } catch (error: Exception) {
            val failure =
                if (error is ReceiptFileException) {
                    Failure.InvalidInput
                } else {
                    remoteFailure(
                        error,
                    )
                }
            if (failure == Failure.Unauthorized) auth.expire(version)
            if (mutation) revision.update { it + 1 }
            Outcome.Error(failure)
        }
    }

    override suspend fun vehicles() =
        request {
            api.vehicles().map {
                require(
                    it.id > 0 && it.name.isNotBlank() && validVehicleDetails(it.vehicleKind, it.fuelTypes, allowUnknown = true),
                )
                Vehicle(it.id, it.name, it.vehicleKind, it.fuelTypes, it.catalogId, it.makeName, it.modelName)
            }
        }

    override suspend fun history(vehicle: Long) = request { api.history(vehicle).domain() }

    override suspend fun match(
        kind: String,
        makeName: String,
        modelName: String,
    ) = request {
        api.catalogMatch(kind, makeName, modelName).match?.let {
            require(
                validCatalogIdentity(kind, it.id) &&
                    it.makeSlug == it.id.split('/')[1] &&
                    it.makeName.isNotBlank() &&
                    it.modelName.isNotBlank(),
            )
            VehicleCatalogMatch(it.id, it.makeSlug, it.makeName, it.modelName)
        }
    }

    override suspend fun search(
        kind: String,
        makeSlug: String?,
        query: String,
    ) = request {
        api.catalog(kind, makeSlug, query).map {
            require(it.id.isNotBlank() && it.name.isNotBlank())
            VehicleCatalogOption(it.id, it.name)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun apply(change: GarageChange): Outcome<Unit> =
        request(true) {
            fun BigDecimal?.json(): JsonPrimitive = this?.let { JsonUnquotedLiteral(it.toPlainString()) } ?: JsonNull

            fun JsonObjectBuilder.pressure(p: TirePressure) {
                put("front_tire_pressure_solo", p.frontSolo.json())
                put("rear_tire_pressure_solo", p.rearSolo.json())
                put("front_tire_pressure_pillion", p.frontPillion.json())
                put("rear_tire_pressure_pillion", p.rearPillion.json())
            }

            fun JsonObjectBuilder.text(
                key: String,
                value: String?,
            ) {
                put(
                    key,
                    value?.trim()?.takeIf(String::isNotEmpty)?.let(::JsonPrimitive) ?: JsonNull,
                )
            }
            when (change) {
                is GarageChange.SaveVehicle ->
                    api.save(
                        change.vehicle?.let { "vehicles/$it" } ?: "vehicles",
                        change.vehicle != null,
                        buildJsonObject {
                            put("name", change.name.trim())
                            put("kind", change.vehicleKind)
                            put("catalog_id", change.catalogId.orEmpty())
                            put("make_name", change.makeName.trim())
                            put("model_name", change.modelName.trim())
                            putJsonArray("fuel_types") { change.fuelTypes.forEach { add(JsonPrimitive(it)) } }
                            if (change.vehicle == null) put("is_active", change.active)
                            pressure(change.pressure)
                        },
                    )
                is GarageChange.Pressure ->
                    api.save(
                        "vehicles/${change.vehicle}/tire-pressure",
                        true,
                        buildJsonObject {
                            pressure(change.value)
                        },
                    )
                is GarageChange.Air ->
                    api.save(
                        "vehicles/${change.vehicle}/air-fills",
                        false,
                        buildJsonObject {
                        },
                    )
                is GarageChange.Fuel ->
                    api.save(
                        "vehicles/${change.vehicle}/fuel-fillups" + (change.id?.let { "/$it" } ?: ""),
                        change.id != null,
                        buildJsonObject {
                            put("odometer_km", change.odometer.json())
                            put("filled_at", change.date.toString())
                            text("station_name", change.station)
                            text("notes", change.notes)
                            putJsonArray("items") {
                                change.items.forEach { item ->
                                    add(
                                        buildJsonObject {
                                            put("fuel_type", item.type)
                                            put("fill_type", item.fill)
                                            // Omit absent operands so the server derives the third value using its existing rules.
                                            item.quantity?.let { put("quantity", it.json()) }
                                            item.price?.let { put("unit_price", it.json()) }
                                            item.total?.let { put("total_cost", it.json()) }
                                        },
                                    )
                                }
                            }
                        },
                    )
                is GarageChange.Care ->
                    api.save(
                        "vehicles/${change.vehicle}/maintenance-records" + (
                            change.id?.let {
                                "/$it"
                            } ?: ""
                        ),
                        change.id != null,
                        buildJsonObject {
                            put("category", change.category)
                            put("title", change.title.trim())
                            put("amount", change.amount.json())
                            put("occurred_at", change.date.toString())
                            put("odometer_km", change.odometer.json())
                            text("provider_name", change.provider)
                            text("notes", change.notes)
                        },
                    )
                is GarageChange.Delete ->
                    api.delete(
                        "vehicles/${change.vehicle}" +
                            when (change.kind) {
                                GarageRecord.Vehicle -> ""
                                GarageRecord.Fuel -> "/fuel-fillups/${change.id}"
                                GarageRecord.Air -> "/air-fills/${change.id}"
                                GarageRecord.Maintenance -> "/maintenance-records/${change.id}"
                                GarageRecord.Receipt -> "/maintenance-records/${change.record}/attachments/${change.id}"
                            },
                    )
            }
        }

    override suspend fun upload(
        vehicle: Long,
        record: Long,
        source: String,
        progress: (Float) -> Unit,
    ) = request(true) {
        val file = files.read(source)
        api.upload(vehicle, record, file.name, file.type, file.bytes, progress)
    }

    override suspend fun download(
        vehicle: Long,
        record: Long,
        receipt: GarageReceipt,
        destination: String?,
    ) = request {
        files.write(api.download(vehicle, record, receipt.id), receipt.name, destination)
    }
}
