package dev.abhishekan.pulse.feature.fitness

import dev.abhishekan.pulse.core.domain.model.*
import java.time.LocalDate
import java.time.YearMonth

data class FitnessUiState(val selected: LocalDate, val month: YearMonth, val overview: FitnessOverview? = null,
    val loading: Boolean = true, val error: Failure? = null, val busy: Boolean = false,
    val message: String? = null, val needsReview: Boolean = false, val reviewReady: Boolean = false,
    val createdId: Long? = null)
