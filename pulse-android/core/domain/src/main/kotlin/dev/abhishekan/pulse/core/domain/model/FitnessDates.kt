package dev.abhishekan.pulse.core.domain.model

import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

object FitnessDates {
    val zone: ZoneId = ZoneId.of("Asia/Kolkata")

    fun today(clock: Clock): LocalDate = LocalDate.now(clock.withZone(zone))

    fun calendar(month: YearMonth): List<LocalDate> {
        val first = month.atDay(1)
        val start = first.minusDays((first.dayOfWeek.value - 1).toLong())
        val cells = ((first.dayOfWeek.value - 1 + month.lengthOfMonth() + 6) / 7) * 7
        return List(cells) { start.plusDays(it.toLong()) }
    }
}
