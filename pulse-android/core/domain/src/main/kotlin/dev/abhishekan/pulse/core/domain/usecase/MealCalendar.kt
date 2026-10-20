package dev.abhishekan.pulse.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class MealCalendar(
    private val clock: Clock,
) {
    fun today(): LocalDate = LocalDate.now(clock.withZone(ZoneId.of("Asia/Kolkata")))

    fun monday(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())

    fun shift(
        date: LocalDate,
        weeks: Long,
    ): LocalDate? =
        runCatching { date.plusWeeks(weeks) }
            .getOrNull()
            ?.takeIf {
                monday(it).year in 1..9999 && monday(it).plusDays(6).year in 1..9999
            }
}
