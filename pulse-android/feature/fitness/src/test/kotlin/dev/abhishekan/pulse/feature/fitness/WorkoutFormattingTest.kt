package dev.abhishekan.pulse.feature.fitness

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutFormattingTest {
    @Test fun durationMatchesWebAtMinuteAndHourBoundaries() {
        mapOf("0" to "0 minutes", "0.001" to "Less than a minute", "59.999" to "Less than a minute",
            "60" to "1 minute", "3599.9" to "59 minutes", "3661.5" to "1 hour 1 minute").forEach { (seconds, expected) ->
            assertEquals(expected, formatWorkoutDuration(seconds.toBigDecimal()))
        }
    }
}
