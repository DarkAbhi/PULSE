package dev.abhishekan.pulse.core.domain.model

import dev.abhishekan.pulse.core.domain.usecase.MealCalendar
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class UserTimeZoneTest {
    @After fun reset() = UserTimeZone.reset()

    @Test fun defaultIsUtcAndSelectionChangesFitnessAndMealToday() {
        UserTimeZone.reset()
        val clock = Clock.fixed(Instant.parse("2026-01-01T00:30:00Z"), ZoneOffset.UTC)
        assertEquals("UTC", UserTimeZone.zone.id)
        assertEquals(LocalDate.of(2026, 1, 1), FitnessDates.today(clock))
        UserTimeZone.select("America/Los_Angeles")
        assertEquals(LocalDate.of(2025, 12, 31), FitnessDates.today(clock))
        assertEquals(LocalDate.of(2025, 12, 31), MealCalendar(clock).today())
        UserTimeZone.select("Pacific/Kiritimati")
        assertEquals(LocalDate.of(2026, 1, 1), FitnessDates.today(clock))
    }

    @Test fun selectedZoneUsesDaylightSavingRules() {
        UserTimeZone.select("America/New_York")
        val before = Instant.parse("2026-03-08T06:59:59Z").atZone(UserTimeZone.zone)
        val after = Instant.parse("2026-03-08T07:00:00Z").atZone(UserTimeZone.zone)
        assertEquals(1, before.hour)
        assertEquals(3, after.hour)
        UserTimeZone.reset()
        assertEquals("UTC", UserTimeZone.zone.id)
    }
}
