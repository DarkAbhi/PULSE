package dev.abhishekan.pulse.feature.mealplan

import dev.abhishekan.pulse.core.domain.model.*
import java.time.LocalDate
import java.time.LocalTime

data class MealDraft(val timeForm: Boolean = false, val id: Long? = null, val name: String = "", val date: String = "",
    val timeId: Long? = null, val originalTimeId: Long? = null, val start: String = "16:00", val end: String = "17:00", val dirty: Boolean = false) {
    fun saved() = arrayListOf(timeForm.toString(), id?.toString().orEmpty(), name, date, timeId?.toString().orEmpty(),
        originalTimeId?.toString().orEmpty(), start, end, dirty.toString())
    fun change(): MealChange = if (timeForm) MealChange.CreateTime(name, LocalTime.parse(start), LocalTime.parse(end))
        else MealChange.Save(id, LocalDate.parse(date), name, timeId)
    companion object {
        fun restore(v: List<String>) = MealDraft(v[0].toBoolean(), v[1].toLongOrNull(), v[2], v[3], v[4].toLongOrNull(), v[5].toLongOrNull(), v[6], v[7], v[8].toBoolean())
        fun edit(meal: PlannedMeal) = MealDraft(id = meal.id, name = meal.name, date = meal.date.toString(), timeId = meal.timeId, originalTimeId = meal.timeId)
    }
}
