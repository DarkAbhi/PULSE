package dev.abhishekan.pulse.feature.fitness.workout

import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiPlural
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.fitness.R
import java.math.BigDecimal
import java.math.BigInteger

/** Matches the web's whole-minute duration presentation without floating-point conversion. */
internal fun formatWorkoutDuration(
    seconds: BigDecimal,
    compact: Boolean = false,
): UiText {
    if (seconds.signum() > 0 &&
        seconds < BigDecimal(60)
    ) {
        return uiText(
            if (compact) R.string.pulse_fitness_workout_duration_under_minute else R.string.pulse_fitness_less_than_a_minute,
        )
    }
    val minutes = seconds.divideToIntegralValue(BigDecimal(60)).toBigInteger()
    val (hours, remainder) = minutes.divideAndRemainder(BigInteger.valueOf(60))
    if (compact) {
        return if (hours.signum() == 0) {
            uiText(R.string.pulse_fitness_workout_duration_short_minutes, remainder)
        } else {
            uiText(R.string.pulse_fitness_workout_duration_short_hours, hours, remainder)
        }
    }
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
