package dev.abhishekan.pulse.feature.fitness

import java.math.BigDecimal
import java.math.BigInteger

/** Matches the web's whole-minute duration presentation without floating-point conversion. */
internal fun formatWorkoutDuration(seconds: BigDecimal): String {
    if (seconds.signum() > 0 && seconds < BigDecimal(60)) return "Less than a minute"
    val minutes = seconds.divideToIntegralValue(BigDecimal(60)).toBigInteger()
    val (hours, remainder) = minutes.divideAndRemainder(BigInteger.valueOf(60))
    val minuteLabel = "$remainder ${if (remainder == BigInteger.ONE) "minute" else "minutes"}"
    return if (hours.signum() == 0) minuteLabel else "$hours ${if (hours == BigInteger.ONE) "hour" else "hours"} $minuteLabel"
}
