package dev.abhishekan.pulse.feature.garage.maintenance.reminders

import dev.abhishekan.pulse.core.domain.model.VehicleHistory

internal enum class ReminderPage(
    val savedKey: String,
) {
    List("list"),
    EngineOil("engine_oil"),
    TirePressure("tire_pressure"),
    GeneralService("general_service"),
    ;

    companion object {
        fun fromSavedKey(key: String?) = entries.firstOrNull { it.savedKey == key }

        fun forSchedule(kind: String) = fromSavedKey(kind)?.takeUnless { it == List }
    }
}

internal fun VehicleHistory.editableSchedules() =
    schedules.filter { schedule ->
        when (ReminderPage.forSchedule(schedule.kind)) {
            ReminderPage.EngineOil -> fuelTypes.any { it != "electric" }
            ReminderPage.TirePressure, ReminderPage.GeneralService -> true
            ReminderPage.List, null -> false
        }
    }
