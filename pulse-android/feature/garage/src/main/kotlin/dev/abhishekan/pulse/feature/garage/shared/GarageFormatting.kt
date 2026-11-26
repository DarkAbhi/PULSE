package dev.abhishekan.pulse.feature.garage.shared

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun decimal(v: BigDecimal?) = v?.stripTrailingZeros()?.toPlainString() ?: "—"

private val garageDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm")

internal fun garageDate(v: Instant): String =
    garageDateFormatter.withLocale(Locale.getDefault(Locale.Category.FORMAT)).withZone(UserTimeZone.zone).format(v)
