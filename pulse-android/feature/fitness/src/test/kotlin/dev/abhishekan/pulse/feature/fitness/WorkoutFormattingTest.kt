package dev.abhishekan.pulse.feature.fitness

import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.core.ui.uiPlural
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class WorkoutFormattingTest {
    @Test fun durationMatchesWebAtMinuteAndHourBoundaries() {
        val zero = uiPlural(R.plurals.pulse_fitness_duration_minutes, 0, BigInteger.ZERO)
        val one = uiPlural(R.plurals.pulse_fitness_duration_minutes, 1, BigInteger.ONE)
        val short = uiText(R.string.pulse_fitness_less_than_a_minute)
        mapOf("0" to zero, "0.001" to short, "59.999" to short,
            "60" to one, "3599.9" to uiPlural(R.plurals.pulse_fitness_duration_minutes,59,BigInteger.valueOf(59)),
            "3661.5" to uiPlural(R.plurals.pulse_fitness_duration_hours,1,BigInteger.ONE,one)).forEach { (seconds, expected) ->
            assertEquals(expected, formatWorkoutDuration(seconds.toBigDecimal()))
        }
    }
}
