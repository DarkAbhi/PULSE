package dev.abhishekan.pulse.core.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class MealTime(val id: Long, val name: String, val start: LocalTime, val end: LocalTime, val isDefault: Boolean)
data class PlannedMeal(val id: Long, val date: LocalDate, val name: String, val timeId: Long?, val timeName: String?,
    val start: LocalTime?, val end: LocalTime?, val consumed: Boolean)
sealed interface MealChange {
    data class Save(val id: Long?, val date: LocalDate, val name: String, val timeId: Long?) : MealChange
    data class Delete(val id: Long) : MealChange
    data class CreateTime(val name: String, val start: LocalTime, val end: LocalTime) : MealChange
    data class DeleteTime(val time: MealTime) : MealChange
}
