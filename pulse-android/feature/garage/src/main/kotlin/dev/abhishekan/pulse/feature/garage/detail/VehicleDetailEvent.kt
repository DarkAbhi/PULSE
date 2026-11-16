package dev.abhishekan.pulse.feature.garage.detail

internal sealed interface VehicleDetailEvent {
    data class OpenReceipt(val uri: String, val contentType: String) : VehicleDetailEvent
}
