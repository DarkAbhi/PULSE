package dev.abhishekan.pulse.core.domain.repository
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.Vehicle
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import kotlinx.coroutines.flow.StateFlow

interface GarageRepository {
    val changes: StateFlow<Long>

    suspend fun vehicles(): Outcome<List<Vehicle>>

    suspend fun history(vehicle: Long): Outcome<VehicleHistory>

    suspend fun apply(change: GarageChange): Outcome<Unit>

    suspend fun upload(
        vehicle: Long,
        record: Long,
        source: String,
        progress: (Float) -> Unit,
    ): Outcome<Unit>

    // Opaque document locations are resolved by the Android infrastructure layer.
    suspend fun download(
        vehicle: Long,
        record: Long,
        receipt: GarageReceipt,
        destination: String?,
    ): Outcome<String>
}
