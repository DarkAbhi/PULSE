package dev.abhishekan.pulse.feature.fitness

import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiPlural
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.fitness.R
import java.math.BigDecimal
import java.math.BigInteger

/** Matches the web's whole-minute duration presentation without floating-point conversion. */
internal fun formatWorkoutDuration(seconds: BigDecimal): UiText {
    if (seconds.signum() > 0 &&
        seconds < BigDecimal(60)
    ) {
        return uiText(R.string.pulse_fitness_less_than_a_minute)
    }
    val minutes = seconds.divideToIntegralValue(BigDecimal(60)).toBigInteger()
    val (hours, remainder) = minutes.divideAndRemainder(BigInteger.valueOf(60))
    val minuteLabel =
        uiPlural(R.plurals.pulse_fitness_duration_minutes, remainder.toInt(), remainder)
    val quantity =
        if (hours.bitLength() <
            31
        ) {
            hours.toInt()
        } else {
            1_000_000_000 + hours.mod(BigInteger.valueOf(1_000_000_000)).toInt()
        }
    return if (hours.signum() ==
        0
    ) {
        minuteLabel
    } else {
        uiPlural(R.plurals.pulse_fitness_duration_hours, quantity, hours, minuteLabel)
    }
}
