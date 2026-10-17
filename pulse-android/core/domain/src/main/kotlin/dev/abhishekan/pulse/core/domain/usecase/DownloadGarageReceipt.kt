package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.RECEIPT_LIMIT
import dev.abhishekan.pulse.core.domain.repository.GarageRepository

class DownloadGarageReceipt(
    private val repository: GarageRepository,
) {
    suspend operator fun invoke(
        vehicle: Long,
        record: Long,
        receipt: GarageReceipt,
        destination: String?,
    ) = if (vehicle >
        0 &&
        record > 0 &&
        receipt.id > 0 &&
        receipt.size in 1..RECEIPT_LIMIT.toLong()
    ) {
        repository.download(vehicle, record, receipt, destination)
    } else {
        Outcome.Error(Failure.InvalidInput)
    }
}
