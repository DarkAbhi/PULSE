package dev.abhishekan.pulse.core.domain.model

import java.math.BigDecimal
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class FitnessRulesTest {
    @Test fun indiaDateChangesAt1830UtcIncludingYearBoundary() {
        fun today(instant: String) = FitnessDates.today(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC))
        assertEquals(LocalDate.of(2026, 12, 31), today("2026-12-31T18:29:59Z"))
        assertEquals(LocalDate.of(2027, 1, 1), today("2026-12-31T18:30:00Z"))
    }
    @Test fun calendarIncludesAdjacentDatesAndCompleteMondayWeeks() {
        val february = FitnessDates.calendar(YearMonth.of(2024, 2))
        assertEquals(35, february.size)
        assertEquals(LocalDate.of(2024, 1, 29), february.first())
        assertEquals(LocalDate.of(2024, 3, 3), february.last())
        assertTrue(february.contains(LocalDate.of(2024, 2, 29)))
        assertEquals(LocalDate.of(2026, 1, 4), FitnessDates.calendar(YearMonth.of(2025, 12)).last())
        assertEquals(42, FitnessDates.calendar(YearMonth.of(2026, 3)).size)
    }
    private val valid = ExerciseInput("Squat", listOf(ExerciseSetInput(10, null)), null, false)
    @Test fun exerciseRulesCoverBackendLimitsWithoutCountingSurrogatesTwice() {
        assertNull(valid.copy(name = "🏋".repeat(100)).issue)
        assertEquals(ExerciseIssue.Name, valid.copy(name = "🏋".repeat(101)).issue)
        assertEquals(ExerciseIssue.Name, valid.copy(name = "  ").issue)
        assertEquals(ExerciseIssue.Sets, valid.copy(sets = emptyList()).issue)
        assertEquals(ExerciseIssue.Sets, valid.copy(sets = List(21) { ExerciseSetInput(1, null) }).issue)
        assertNull(valid.copy(sets = List(20) { ExerciseSetInput(1000, BigDecimal.ZERO) }).issue)
        assertEquals(ExerciseIssue.Reps, valid.copy(sets = listOf(ExerciseSetInput(0, null))).issue)
        assertEquals(ExerciseIssue.Reps, valid.copy(sets = listOf(ExerciseSetInput(1001, null))).issue)
        assertEquals(ExerciseIssue.Weight, valid.copy(sets = listOf(ExerciseSetInput(1, BigDecimal("-0.00001")))).issue)
        assertEquals(ExerciseIssue.Weight, valid.copy(sets = listOf(ExerciseSetInput(1, BigDecimal("1E309")))).issue)
        assertEquals(ExerciseIssue.Alias, valid.copy(rememberAlias = true).issue)
        assertEquals(ExerciseIssue.Catalog, valid.copy(catalogId = "é".repeat(101)).issue)
    }
}
