package dev.abhishekan.pulse.core.data
import dev.abhishekan.pulse.core.domain.model.MealTime
import dev.abhishekan.pulse.core.domain.model.PlannedMeal
import dev.abhishekan.pulse.core.network.dto.MealTimeDto
import dev.abhishekan.pulse.core.network.dto.PlannedMealDto
import java.time.LocalDate
import java.time.LocalTime

internal fun MealTimeDto.domain(): MealTime {
    require(id > 0 && name.isNotBlank())
    return MealTime(id, name, LocalTime.parse(start), LocalTime.parse(end), isDefault)
}

internal fun PlannedMealDto.domain(): PlannedMeal {
    require(id > 0 && name.isNotBlank() && (timeId?.let { it > 0 } ?: true))
    val day = LocalDate.parse(date)
    require(day.year in 1..9999)
    return PlannedMeal(
        id,
        day,
        name,
        timeId,
        timeName?.takeIf(String::isNotBlank),
        start?.takeIf(String::isNotBlank)?.let(LocalTime::parse),
        end?.takeIf(String::isNotBlank)?.let(LocalTime::parse),
        consumed,
    )
}
