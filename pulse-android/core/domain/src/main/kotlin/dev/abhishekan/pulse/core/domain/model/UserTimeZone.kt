package dev.abhishekan.pulse.core.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZoneId

/** Account preference, loaded before authenticated screens are shown. */
object UserTimeZone {
    private val selected = MutableStateFlow(ZoneId.of("UTC"))
    val changes = selected.asStateFlow()
    val zone: ZoneId get() = selected.value
    val available: List<String> = (ZoneId.getAvailableZoneIds().filter { "/" in it && !it.startsWith("SystemV/") } + "UTC").sorted()

    fun select(id: String) {
        selected.value = ZoneId.of(id)
    }

    fun reset() = select("UTC")
}
