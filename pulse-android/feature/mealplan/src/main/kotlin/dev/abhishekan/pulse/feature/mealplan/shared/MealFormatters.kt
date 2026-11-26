package dev.abhishekan.pulse.feature.mealplan.shared

import java.time.format.DateTimeFormatter

internal val mealClock: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
internal val mealDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy")
